package control;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.Sound;

/**
 *
 * @author Philipp Schulz
 */
public class InputProcessor
{
    private SIMON simon;
    private final Sound sound;
    private final VoiceRecognition voiceRecognition;
    private final Runtime runtime;
    private int dayOfYear;
    private volatile Instant movementTimerStart;
    private volatile LocalDateTime now;
    private volatile boolean sleepMode;
    private volatile boolean active = false;
    private volatile boolean running = true;
    private volatile boolean isEye = false;
    private volatile boolean threadRun = true;
    private volatile boolean threadSleep = false;
    private volatile boolean isPaused = false;
    private volatile long pausedTimestamp;
    private volatile long currentTimestamp;
    
    //Strings for replacing lettuce with GLaDOS
    private final String gladosWrong = "lettuce";
    private final String gladosRight = "GLaDOS";
    /**
     * Constructor
     */
    public InputProcessor() {
        this.pausedTimestamp = new Timestamp(System.currentTimeMillis()).getTime();
        this.currentTimestamp = new Timestamp(System.currentTimeMillis()).getTime();
        this.movementTimerStart=Instant.now();
        this.now = LocalDateTime.now();  
        this.dayOfYear=now.getDayOfYear();
        
        this.sleepMode=false;
        this.runtime = Runtime.getRuntime();
        this.sound = new Sound();
        if(!sound.isWindows)
        {
            this.simon = new SIMON();
            simon.beginSIMON();
        }
        else
        {
            this.simon=null;
        }
        this.voiceRecognition = new VoiceRecognition(sound);
        try
        {
            //tempRunning();
            running();
        } catch (Exception e)
        {
            Logger.getLogger(InputProcessor.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    
    public void tempRunning()
    {
        simon.sendSIMONData(2, "2 1");
    }
    
    /**
     * main entry point of program
     */
    public void running() throws IOException
    {
        /*
        Control of Body for sending over SIMON:
        Control Eye:    "1 0" or "1 1"
        Control LEDs:   "2 0" or "2 1"
        Control Servos: "3 0" or "3 1"
        Change Servos:  "4 0 0 0 0" to "4 9 9 9 9"
        */
        sound.playSound("initiate");
        sound.waitForSoundBeingFinished();
        voiceRecognition.turnOnRecognition();
        //basic functionality is done, only the specific actions for each input are required
        
        //Thread for handling turn off display and body with timers
        Thread thread = new Thread()
        {
            @Override
            public void run()
            {
                while(threadRun)
                {
                    while(!threadSleep && !sleepMode)
                    {
                        if((Duration.between(movementTimerStart, Instant.now()).toMinutes()>=5&&!active))
                        {
                            movementTimerStart=Instant.now();
                            System.out.println("movement timer");
                            simon.sendSIMONData(2, "1 1");
                            int position1=(int)(Math.random()*10);
                            int position2=(int)(Math.random()*10);
                            int position3=(int)(Math.random()*10);
                            int position4=(int)(Math.random()*10);
                            simon.sendSIMONData(2, "4 "+position1+" "+position2+" "+position3+" "+position4);
                            simon.sendSIMONData(2, "1 0");
                        }
                        try
                        {
                            Thread.sleep(1000);
                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                    try
                    {
                        Thread.sleep(1000);
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();
        
        while(running)
        {
            if(!sleepMode && !isPaused)
            {
                String result = voiceRecognition.recognize();
                if(result.contains(gladosWrong))
                {
                    System.out.println("Input: "+result.replace(gladosWrong,gladosRight));
                }
                else
                {
                    System.out.println("Input: "+result);
                }
                if(result.equals("hey lettuce")||result.equals("okay lettuce"))
                {
                    active = true;
                    sound.playSound("yes");
                    runtime.exec("xset -display :0 s reset");
                    if(!isEye)
                    {
                        simon.sendSIMONData(2, "1 1");
                        int position1=(int)(Math.random()*10);
                        int position2=(int)(Math.random()*10);
                        int position3=(int)(Math.random()*10);
                        int position4=(int)(Math.random()*10);
                        simon.sendSIMONData(2, "4 "+position1+" "+position2+" "+position3+" "+position4);
                    }
                }
                else if(result.equals("abort") && active)
                {
                    sound.playSound("okay");
                    runtime.exec("xset -display :0 s activate");
                    active = false;
                }
                else if(result.equals("hello lettuce")||result.equals("hi lettuce"))
                {
                    sound.playSound("hello");
                } 
                else if(result.equals("turn lights on") && active)
                {
                    active = false;
                    sound.playSound("turning the lights on");
                    sound.waitForSoundBeingFinished();
                    simon.sendSIMONData(2, "2 1");
                    runtime.exec("xset -display :0 s activate");
                } 
                else if(result.equals("turn lights off") && active)
                {
                    active = false;
                    sound.playSound("turning the lights off");
                    sound.waitForSoundBeingFinished();
                    simon.sendSIMONData(2, "2 0");
                    runtime.exec("xset -display :0 s activate");
                }
                else if((result.equals("turn off")) && active)
                {
                    sound.playSound("Really");
                    sound.waitForSoundBeingFinished();
                    String nextResult = voiceRecognition.recognize();
                    if(nextResult.equals("yes"))
                    {
                        threadRun=false;
                        threadSleep=true;
                        running = false;
                        sound.playSound("okay");
                        simon.sendSIMONData(2, "1 0");
                        simon.sendSIMONData(2, "2 0");
                        simon.sendSIMONData(2, "3 0");
                        simon.sendSIMONData(2, "4 5 7 5 5");
                    }
                    active = false;
                    runtime.exec("xset -display :0 s activate");
                }
                else if(result.equals("shutdown") && active)
                {
                    sound.playSound("Really");
                    sound.waitForSoundBeingFinished();
                    String nextResult = voiceRecognition.recognize();
                    if(nextResult.equals("yes"))
                    {
                        threadRun=false;
                        threadSleep=true;
                        running = false;
                        sound.playSound("okay");
                        simon.sendSIMONData(2, "1 0");
                        simon.sendSIMONData(2, "2 0");
                        simon.sendSIMONData(2, "3 0");
                        simon.sendSIMONData(2, "4 5 7 5 5");
                        runtime.exec("xset -display :0 s activate");
                        if(sound.isWindows)
                        {
                            runtime.exec("shutdown /s -t 1");
                        }
                        else
                        {
                            runtime.exec("sudo shutdown now");
                        }
                    }
                    active = false;
                    runtime.exec("xset -display :0 s activate");
                } 
                else if(result.equals("reboot") && active)
                {
                    sound.playSound("Really");
                    sound.waitForSoundBeingFinished();
                    String nextResult = voiceRecognition.recognize();
                    if(nextResult.equals("yes"))
                    {
                        threadRun=false;
                        threadSleep=true;
                        running = false;
                        sound.playSound("okay");
                        simon.sendSIMONData(2, "1 0");
                        simon.sendSIMONData(2, "2 0");
                        simon.sendSIMONData(2, "3 0");
                        simon.sendSIMONData(2, "4 5 7 5 5");
                        runtime.exec("xset -display :0 s activate");
                        if(sound.isWindows)
                        {
                            runtime.exec("shutdown /r -t 1");
                        }
                        else
                        {
                            runtime.exec("sudo reboot now");
                        }
                    }
                    active = false;
                    runtime.exec("xset -display :0 s activate");
                } 
                else if(result.equals("turn eye on") && active)
                {
                    active = false;
                    sound.playSound("okay");
                    simon.sendSIMONData(2, "1 1");
                    runtime.exec("xset -display :0 s activate");
                } 
                else if(result.equals("turn eye off") && active)
                {
                    active = false;
                    sound.playSound("okay");
                    simon.sendSIMONData(2, "1 0");
                    runtime.exec("xset -display :0 s activate");
                } 
                else if(result.equals("enable sleep mode") && active)
                {
                    sound.playSound("Really");
                    String nextResult = voiceRecognition.recognize();
                    if(nextResult.equals("yes"))
                    {
                        threadSleep=true;
                        sleepMode=true;
                        now = LocalDateTime.now();  
                        dayOfYear=now.getDayOfYear();
                        sound.playSound("okay");
                        simon.sendSIMONData(2, "1 0");
                        simon.sendSIMONData(2, "2 0");
                        simon.sendSIMONData(2, "3 0");
                        simon.sendSIMONData(2, "4 5 7 5 5");
                        voiceRecognition.turnOffRecognition();
                    }
                    active = false;
                }
                else if(result.equals("pause recognition")||result.equals("shut up"))
                {
                    sound.playSound("Really");
                    sound.waitForSoundBeingFinished();
                    String nextResult = voiceRecognition.recognize();
                    if(nextResult.equals("yes"))
                    {
                        isPaused = true;
                        sound.playSound("okay");
                        pausedTimestamp = new Timestamp(System.currentTimeMillis()).getTime();
                        voiceRecognition.turnOffRecognition();
                    }
                    active = false;
                    runtime.exec("xset -display :0 s activate");
                }
                else if(result.equals("reset communication"))
                {
                    sound.playSound("okay");
                    simon.resetSIMON();
                    sound.waitForSoundBeingFinished();
                    sound.playSound("please continue");
                    active = false;
                    runtime.exec("xset -display :0 s activate");
                }
                sound.waitForSoundBeingFinished();
            }
            else if(isPaused)   //if recognition was paused
            {
                System.out.println("paused loop");
                currentTimestamp = new Timestamp(System.currentTimeMillis()).getTime();
                if(isPaused && currentTimestamp>=pausedTimestamp+3600000)
                {
                    simon.resetSIMON();
                    System.out.println("end of paused loop reached");
                    isPaused = false;
                    sleepMode = false;
                    sound.playSound("please continue");
                    sound.waitForSoundBeingFinished();
                    voiceRecognition.turnOnRecognition();
                }
                try
                {
                    Thread.sleep(60000);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
            else        //if sleep mode was enabled
            {
                now = LocalDateTime.now();
                if(dayOfYear<now.getDayOfYear() && now.getHour()==9)
                {
                    simon = new SIMON();
                    simon.beginSIMON();
                    runtime.exec("xset -display :0 s activate");
                    threadSleep=false;
                    sleepMode=false;
                    simon.sendSIMONData(2, "1 1");
                    int position1=(int)(Math.random()*10);
                    int position2=(int)(Math.random()*10);
                    int position3=(int)(Math.random()*10);
                    int position4=(int)(Math.random()*10);
                    simon.sendSIMONData(2, "4 "+position1+" "+position2+" "+position3+" "+position4);
                    simon.sendSIMONData(2, "1 0");
                    voiceRecognition.turnOnRecognition();
                }
                try
                {
                    Thread.sleep(100000);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}