package model;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;

/**
 * Temporary Sound Class for playing sounds corresponding to words and numbers in a given string
 * @author Philipp Schulz
 */
public class Sound
{
    public boolean isWindows;
    private ArrayList<AudioInputStream> clips;
    
    private final int MINUS1 = -1;
    private final int TRIPLET_OFFSET = 3;
    private final int TWENTY = 20;
    private final int DECIMAL_ROUND = 4;
    private final int ZERO_INT=0;
    private final String EMPTY_STRING = "";
    private final String SPACER = " ";
    private final String DOT = ".";
    private final String ZERO = "0";
    private final String TRIPLET_ZERO = "000";
    private final int T0=0;
    private final int T1=1;
    private final int T2=2;
    private final String MINUS = "minus";
    private final String DOT_WORD = "point";
    private final String HUNDRED = "hundred";
    private final String[] SINGLE_DIGITS = {"zero","one","two","three","four","five","six","seven",
        "eight","nine"};
    private final String[] UNDER_TWENTY = {"","one","two","three","four","five","six","seven",
        "eight","nine","ten","eleven","twelve","thirteen","fourteen","fifteen",
        "sixteen","seventeen","eighteen","nineteen"};
    private final String[] TEENS_DIGITS = {"","","twenty","thirty","fourty","fifty","sixty","seventy",
        "eighty","ninety"};
    private final String[] TEN_STEPS = {"","","thousand","million","billion"};
    
    private Mixer mixer;
    private volatile boolean hasFinished;
    private File file;
    private File homeDir;
    private Clip clip;
    private AudioInputStream audioIn;
    private URL url;
    private ArrayList<String> messages;
    
    private final String USER_DIR = "user.dir";
    private final String SLASH = "/";
    
    private final int INITIAL_INDEX = 0;
    private final int WAITING_TIME_SOUND = 5;
    private final String MIXER_RASPI_INFO = "Direct Audio Device: USB Audio Device, USB Audio, USB Audio";
    
    private final String GLADOS_OUTPUT = "GLaDOS: ";
    private final String FILE_PATH = "source/glados/";
    private final String PI_DIR = "/home/pi/";
    private final String AUDIO_FORMAT = ".wav";
    private final String SPACING = " ";
    private final String ERROR_WAITING = "ney ";
    private final int WAITING_TIME = 10;
    private final String POINT = "point";
    
    public Sound()
    {
        determineOperatingSystem();
        initialize();
    }
    
    /**
     * Initializes the Sound Class for specific outputs because the sound output on the Raspberry Pi0W is stupid
     * @author Philipp Schulz
     */
    private void initialize()
    {
        clips = new ArrayList<>();
        Mixer.Info[] mixInfos = AudioSystem.getMixerInfo();
        if(!isWindows)
        {
            int index = INITIAL_INDEX;
            for(int i = 0; i < mixInfos.length; i++)
            {
                if(mixInfos[i].getDescription().equals(MIXER_RASPI_INFO))
                {
                    index = i;
                    break;
                }
            }
            mixer = AudioSystem.getMixer(mixInfos[index]);
            DataLine.Info dataInfo = new DataLine.Info(Clip.class, null);
            try
            {
                clip = (Clip) mixer.getLine(dataInfo);
            }
            catch(LineUnavailableException lue)
            {
                lue.printStackTrace();
            }
        }
        else
        {
            try
            {
                clip = AudioSystem.getClip();
            }
            catch(LineUnavailableException ex)
            {
                ex.printStackTrace();
            }
        }
    }
    
    /**
     * Call this method to turn a given String into audial response of the program
     * @author Philipp Schulz
     * @param msg String that should be given out as audio of this program
     */
    public void playSound(String msg)
    {
        hasFinished = false;
        homeDir = new File(System.getProperty(USER_DIR));
        refeshWordBuffer();
        snipMessage(msg);
        Thread thread = new Thread()
        {
            @Override
            public void run()
            {
                System.out.println(GLADOS_OUTPUT + msg);
                for(int i = 0; i < messages.size(); i++)
                {
                    String fileName = EMPTY_STRING;
                    if(isWindows)
                    {
                        fileName = homeDir.getAbsolutePath() + SLASH;
                    }
                    String wordName = messages.get(i);
                    if(wordName.equals(DOT))
                    {
                        wordName = POINT;
                    }
                    fileName += FILE_PATH + wordName + AUDIO_FORMAT;
                    try
                    {
                        if(!isWindows)
                        {
                            fileName = PI_DIR+fileName;
                        }
                        createFile(fileName);
                        clips.add(audioIn);
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                for(AudioInputStream currentClip : clips)
                {
                    try
                    {
                        clip.open(currentClip);
                        clip.start();
                        waitForEnd();
                        clip.close();
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                hasFinished = true;
            }
        };
        thread.start();
    }
    
    /**
     * Used in the AudioSystem when a specific audio file should be played
     * @author Philipp Schulz
     * @param fileName Name of the file that should be played
     */
    private void createFile(String fileName)
    {
        if(homeDir.getAbsolutePath().equals(PI_DIR))
        {
            try
            {
                file = new File(homeDir, fileName);
                url = file.toURI().toURL();
                audioIn = AudioSystem.getAudioInputStream(url);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            try
            {
                file = new File(fileName);
                url = file.toURI().toURL();
                audioIn = AudioSystem.getAudioInputStream(url);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Used to check if a sound clip has finished playing
     * @author Philipp Schulz
     * @return Whether the sound has finished playing
     */
    public synchronized boolean hasFinished()
    {
        return hasFinished;
    }
    
    /**
     * Method to empty the ArrayList that contains all words from the given String
     * @author Philipp Schulz
     */
    private void refeshWordBuffer()
    {
        messages = new ArrayList<String>();
    }
    
    /**
     * Call this method to block for the time that a sound is still played
     * @author Philipp Schulz
     */
    public void waitForSoundBeingFinished()
    {
        while(!hasFinished())
        {
            try
            {
                Thread.sleep(WAITING_TIME_SOUND);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Splits the given String into every Substring, e.g. words
     * @author Philipp Schulz
     * @param msg String that should be split into all the substrings
     */
    private void snipMessage(String msg)
    {
        try
        {
            int firstIndex = msg.indexOf(SPACING);
            if(firstIndex > 0)
            {
                String restOfMsg = msg.substring((firstIndex + 1), msg.length());
                String firstWord = msg.substring(0, firstIndex);
                firstWord = firstWord.replaceAll(SPACING,EMPTY_STRING);
                try
                {
                   double doubleValue = Double.parseDouble(firstWord);
                   if((doubleValue*10)%10 == 0)
                   {
                        firstWord = numberToString(Integer.parseInt(firstWord)).trim();
                        messages.add(firstWord);
                   }
                   else
                   {
                        firstWord = numberToString(doubleValue).trim();
                        snipMessage(firstWord);
                   }
                   
                }
                catch(Exception e)
                {
                    messages.add(firstWord);
                }
                snipMessage(restOfMsg);
            }
            else
            {
                try
                {
                   double doubleValue = Double.parseDouble(msg);
                   if((doubleValue*10)%10 == 0)
                   {
                        msg = numberToString(Integer.parseInt(msg)).trim();
                        messages.add(msg);
                   }
                   else
                   {
                        msg = numberToString(doubleValue).trim();
                        snipMessage(msg);
                   }
                   
                }
                catch(Exception e)
                {
                    messages.add(msg);
                }
            }
        }
        catch(Exception e)
        {
            
        }
    }
    
    /**
     * Call this method to wait for a specific time
     * @author Philipp Schulz
     */
    public void waitASec()
    {
        try
        {
            Thread.sleep(WAITING_TIME);
        }
        catch(Exception e)
        {
            System.out.println(ERROR_WAITING + e);
        }
    }
    
    /**
     * When this method is called, the program will wait until the currently active audio file has finished playing
     * @author Philipp Schulz
     */
    private void waitForEnd()
    {
        do
        {
            waitASec();
        }while(clip.isActive());
    }
    
    /**
     * Converts a number into its single digits, supports numbers up to 999 billion
     * @author Philipp Schulz
     * @param number Number that should be converted into its single digits
     * @return String that contains the converted number
     */
    public String numberToSingleDigits(String number)
    {
        String newNumber = EMPTY_STRING;
        for(int i = 0; i < number.length(); i++)
        {
            String singleDigit = number.substring(i, i+1);
            if(!singleDigit.equals(DOT))
            {
                newNumber += SINGLE_DIGITS[Integer.parseInt(singleDigit)]+SPACER;
            }
            else
            {
                newNumber += DOT_WORD + SPACER;
            }
        }
        return newNumber;        
    }
    
    /**
     * converts a number into the English language
     * @author Philipp Schulz
     * @param number Number that should be converted
     * @return String that contains the given number in words
     */
    public String numberToString(int number)
    {
        return numberToString(number+EMPTY_STRING);
    }
    
    /**
     * converts a number into the English language
     * @author Philipp Schulz
     * @param number Number that should be converted
     * @return String that contains the given number in words
     */
    public String numberToString(double number)
    {
        return numberToString(number+EMPTY_STRING);
    }
    
    /**
     * Converts a number into the English language
     * @author Philipp Schulz
     * @param number Number that should be converted
     * @return String that contains the given number in words
     */
    public String numberToString(String number)
    {
        String finishedNumber = EMPTY_STRING;
        double numberDouble = Double.parseDouble(number);
        long numberLong = (long)numberDouble;
        if(numberDouble==ZERO_INT)
        {
            finishedNumber = SINGLE_DIGITS[ZERO_INT]+SPACER;
        }
        else
        {
            if(numberLong==ZERO_INT)
            {
                finishedNumber = SINGLE_DIGITS[ZERO_INT]+SPACER;
            }
            if(isNegative(numberLong))
            {
                finishedNumber += MINUS + SPACER;
                numberDouble *=MINUS1;
                numberLong *=MINUS1;

            }
            String allDigits = numberLong + EMPTY_STRING;
            String decimal = number.substring(allDigits.length());
            if(decimal.length() > DECIMAL_ROUND)
            {
                decimal = decimal.substring(0, DECIMAL_ROUND);
            }
            double numberLength = allDigits.length();
            int tripletCount = (int)numberLength/TRIPLET_OFFSET;
            if(tripletCount < numberLength/TRIPLET_OFFSET)
            {
                tripletCount++;
            }
            ArrayList<String> triplets = new ArrayList<>();
            String newAllNumbers = allDigits + EMPTY_STRING;
            for(int i=0;i<tripletCount;i++)
            {
                triplets.add(EMPTY_STRING);
            }
            for(int i=0;i<tripletCount;i++)
            {
                String triplet;
                if((numberLength-(i*TRIPLET_OFFSET)>TRIPLET_OFFSET))
                {
                    int underLimit = newAllNumbers.length()-TRIPLET_OFFSET;
                    triplet = newAllNumbers.substring(underLimit);
                    newAllNumbers = newAllNumbers.substring(0,underLimit);
                }
                else
                {
                    triplet = newAllNumbers;
                }
                triplets.set(tripletCount-i+MINUS1, triplet);
            }
            for(int i = 0; i<triplets.size();i++)
            {
                finishedNumber += convertTripletToWords(triplets.get(i),triplets.size()-i);
            }
            if(!decimal.equals(ZERO))
            {
                String decimalRest = numberToSingleDigits(removeLastZeros(decimal));
                finishedNumber += decimalRest;
            }
        }
        return finishedNumber;
    }
    
    /**
    * Converts a given triplet of a number into the English words and adds the weight at the end
    * @author Philipp Schulz
    * @param triplet Number that should be converted into the English language
    * @param weight Weight of the number
    * @return Original number converted into English words
    */ 
   private String convertTripletToWords(String triplet, int weight)
   {
       String finishedTriplet = EMPTY_STRING;
       String t1,t2,t3,t23;
       switch(triplet.length())
       {
            case T0:
                t1 = ZERO;
                t2 = ZERO;
                t3 = ZERO;
            case T1:
                t1 = ZERO;
                t2 = ZERO;
                t3 = triplet;
                break;
            case T2:
                t1 = ZERO;
                t2 = triplet.substring(0,T1);
                t3 = triplet.substring(T1);
                break;
            default:
                t1 = triplet.substring(0,T1);
                t2 = triplet.substring(T1,T2);
                t3 = triplet.substring(T2);
                break;
        }
       t23 = t2+t3;
       String t123=t1+t23;
       if(!t123.equals(TRIPLET_ZERO))
       {
           if(Integer.parseInt(t1)!=0)
           {
               finishedTriplet = SINGLE_DIGITS[Integer.parseInt(t1)]+SPACER+HUNDRED+SPACER;
           }
           if(Integer.parseInt(t23)<TWENTY)
           {
               finishedTriplet += UNDER_TWENTY[Integer.parseInt(t23)]+SPACER;
           }
           else
           {
               if(!t2.equals(ZERO))
               {
                   finishedTriplet += TEENS_DIGITS[Integer.parseInt(t2)]+SPACER;
               }
               if(!t3.equals(ZERO))
               {
                   finishedTriplet += UNDER_TWENTY[Integer.parseInt(t3)]+SPACER;
               }
           }
           if(weight>T1)
           {
               finishedTriplet+=TEN_STEPS[weight]+SPACER;
           }
       }
       return finishedTriplet;
   }
    
   /**
     * Method to recursively remove the last zeros in the given String
     * @author Philipp Schulz
     * @param number Number from which the last zeros should be removed from
     * @return String that contains the original number without zeros at the end
     */
    private String removeLastZeros(String number)
    {
        String rest = number;
        if(number.endsWith(ZERO))
        {
            rest = number.substring(0, number.length()-1);
            rest = removeLastZeros(rest);
        }
        return rest;
    }
    
    /**
     * Checks whether a given Integer is negative
     * @author Philipp Schulz
     * @param number Number that should be checked whether it is negative
     * @return True or false whether the given number is negative
     */
    private boolean isNegative(long number)
    {
        return number < 0;
    }
    
    /**
     * Determines whether the OS is Windows or not
     * @author Philipp Schulz
     */
    private void determineOperatingSystem()
    {
        isWindows = System.getProperty("os.name").equals("Windows 10");
    }
    //--------------------------new code combined-------------------------------
}