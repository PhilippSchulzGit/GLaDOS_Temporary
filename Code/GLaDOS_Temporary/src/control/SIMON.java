package control;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalMultipurpose;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
/**
 * Class for the SIMON protocol. Enables program to send data to local microcontroller connected via SIMON
 * @author Philipp Schulz
 */
public class SIMON
{
    //Stuff from the Pi4J library that is used to access the GPIO pins
    private GpioController gpio;
    
    //constants and defines for SIMON protocol
    private volatile GpioPinDigitalMultipurpose dataPin;//Data line for the SIMON protocol
    //private final Pin signalPin;                      //Signal line for the SIMON protocol
    //-> not needed as in this case the signalPin is already connected to system wide ground
    private final int ownNumber;                        //device number of the device this code should run on
    private final int minimumSignalLength;              //time in ms that is the minimum signal length of the SIMON protocol
    private final int timeout;                          //time in ms that will pass until the method getSignalLength() times out
    
    /**
     * Constructor of SIMON class
     */
    public SIMON()
    {
        //instantiate gpio factory
        gpio = GpioFactory.getInstance();
        //constants for SIMON protocol
        this.ownNumber = 1;
        this.minimumSignalLength = 1;
        this.timeout = 200;
    }
    
    /**
     * Reset the SIMON protocol in case if the transmissions are failing
     * @author Philipp Schulz
     */
    public void resetSIMON()
    {
        System.out.println("SIMON: resetting protocol...");
        dataPin.unexport();
        gpio.shutdown();
        gpio = null;
        gpio = GpioFactory.getInstance();
        beginSIMON();
    }
    
    /**
     * Set up the dataPin for SIMON protocol
     * @author Philipp Schulz
     */
    public void beginSIMON()
    {
        try
        {
            //initialize dataPin as input with pullup resistor
            dataPin = gpio.provisionDigitalMultipurposePin(RaspiPin.GPIO_00, PinMode.DIGITAL_INPUT, PinPullResistance.PULL_UP);
            dataPin.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
        }
        catch(Exception e)
        {
            dataPin.setMode(PinMode.DIGITAL_OUTPUT);                            //set the dataPin as an output
            dataPin.low();                                                      //configure dataPin as low
            busyWaitMillis((int)timeout/4);                                     //wait for [timeout/4] ms
            dataPin.high();                                                     //configure dataPin as high
            busyWaitMillis((int)timeout/4);                                     //wait for [timeout/4] ms
            dataPin.setMode(PinMode.DIGITAL_INPUT);
            dataPin.setPullResistance(PinPullResistance.PULL_UP);               //configure data line as an input to enable communication
            dataPin.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
        }
        busyWaitMillis(timeout);                                                //wait for [timeout] ms
    }
    
    /**
     * Method for waiting a given time in milliseconds
     * original from: http://www.rationaljava.com/2015/10/measuring-microsecond-in-java.html
     * @author Philipp Schulz
     * @param millis Time in milliseconds that should be waited for
     */
    private static void busyWaitMillis(long millis){ //seems to be somewhat accurate for us, better use in ranges of 100us for better results
        long micros = millis*1000;                                              //convert ms into us
        long waitUntil = System.nanoTime() + (micros * 1_000);                  //calculate timestamp to stop at
        while(waitUntil > System.nanoTime())                                    //wait until calculated timestamp has been reached
        {
            
        }
    }
    
    /**
     * Method for converting a String containing an ASCII text into a binary String
     * @author Philipp Schulz
     * @param text String that contains the ASCII text to convert
     * @return Binary String of the given String
     */
    private String textToBinary(String text)
    {
        String binary="";                                                       //String that wil contain the binary representation of the text
        for(int i=1;i<=text.length();i++)                                       //get and convert each character of the String
        {
            String character = text.substring(i-1,i);                           //get current character
            String binaryCharacter = new BigInteger(character.getBytes()).toString(2);    //converts the given String into binary
            for(int j = binaryCharacter.length(); j < 8;j++)                    //binary String has to be filled with '0' until length of 8 bits
            {
                binaryCharacter = "0"+binaryCharacter;                          //add '0' in front of binary String
            }
            binary+=binaryCharacter;                                            //add converted character to result String
        }
        return binary;                                                          //return binary representation of text as a String
    }
    
    /**
     * Method for converting a binary String to an ASCII text
     * @author Philipp Schulz
     * @param binary String that contains the binary text to convert
     * @return ASCII String of the given binary text
     */
    private String binaryToText(String binary)
    {
        try
        {
            return new String(new BigInteger(binary, 2).toByteArray());             //convert and return binary String into ASCII String
        }
        catch(Exception e)
        {
            return "";
        }
    }
    
    /**
     * Method for converting an Integer into a binary String
     * @author Philipp Schulz
     * @param receiver_number Integer that should be converted into a binary String
     * @return Binary String of the given Integer
     */
    private String intToBinary(int integer)
    {
        String binary=Integer.toString(integer,2);                              //convert Integer to binary String
        for(int i = binary.length();i<8;i++)                                     //fill the missing leading 0s
        {
            binary = "0" + binary;                                              //add missing leading 0
        }
        return binary;                                                          //return binary String of the given Integer
    }
    
    /**
     * Method for converting a binary String into an integer
     * @author Philipp Schulz
     * @param binary Binary String that should be converted into a binary String
     * @return Integer resulting by the given binary String
     */
    private int binaryToInt(String binary)
    {
        return Integer.parseInt(binary, 2);                                     //convert and return binary String into Integer
    }
    
    /**
     * Sends the content of the binaryString including begin and end signal
     * @author Philipp Schulz
     * @param binaryString Binary String which content should be sent
     */
    private void sendDataString(String binaryString) 
    {
        dataPin.setMode(PinMode.DIGITAL_OUTPUT);                                //set the dataPin as an output
        dataPin.high();                                                         //configure dataPin as high
        busyWaitMillis(2*minimumSignalLength);
        for(int i=0;i<8;i++)                                                    //instead of waiting for 16*minimumSignalLength, toggle DATA line
        {
            dataPin.low();                                                      //change DATA pin to LOW
            busyWaitMillis(minimumSignalLength);                                //delay for minimumSignalLength
            dataPin.high();                                                     //change DATA pin to HIGH
            busyWaitMillis(minimumSignalLength);                                //delay for minimumSignalLength
        }
        busyWaitMillis(5*minimumSignalLength);
        dataPin.low();                                                          //begin enable signal
        busyWaitMillis(3*minimumSignalLength);                                  //wait for 3*minimumSignalLength
        dataPin.high();                                                         //end enable signal
        busyWaitMillis(minimumSignalLength);                                    //wait for minimumSignalLength
        for(int i=0;i<binaryString.length();i++)                                //loop over binary String
        {
            char current=binaryString.charAt(i);                                //get current char from binary String
            dataPin.low();                                                      //begin signal binary String digit
            if(current=='0')                                                    //find out if current char is 0 or 1
            {
                busyWaitMillis(minimumSignalLength);                            //if 0, wait for minimumSignalLength
            }
            else
            {
                busyWaitMillis(2*minimumSignalLength);                          //if 1, wait for 2*minimumSignalLength
            }
            dataPin.high();                                                     //end of signal binary String digit
            busyWaitMillis(minimumSignalLength);                                //wait for minimumSignalLength
        }
        dataPin.low();                                                          //begin disable signal
        busyWaitMillis(4*minimumSignalLength);                                  //wait for 4*minimumSignalLength
        dataPin.high();                                                         //end disable signal
        busyWaitMillis(2);                                                      //wait until receiver device notices last digital level
    }
    
    /**
     * Determines the length of the current signal on the signal line, function blocks when called
     * @author Philipp Schulz
     * @return Signal length in milliseconds
     */
    private int getSignalLength() 
    {
        boolean loopStop=false;                                                 //required boolean value for main loop
        int signalLength=0;                                                     //declare value to return, used in later calculation
        boolean timeoutState=false;                                             //required boolean value for determining if signal was found
        Instant beginTime = Instant.now();                                      //save current timestamp
        while(!loopStop)                                                        //main loop, determines signal length
        {
            if(dataPin.isLow())                                                 //if signal is LOW
            {
                beginTime = Instant.now();                                      //save current timestamp
                Instant endTime = beginTime;                                    //save current timestamp
                boolean checkPoint=false;                                       //variable to end while loop
                while(!checkPoint)                                              //when signal is still LOW
                {
                    if(dataPin.isHigh())                                        //if signal ends (e.g. goes from low to high)
                    {
                        checkPoint=true;                                        //break main loop
                        endTime= Instant.now();                                 //save current time to deterime signal length
                    }
                    if((Duration.between(beginTime, Instant.now()).toMillis()/minimumSignalLength)>=timeout)    //timeout for when the signal is not continued
                    {
                        timeoutState=true;                                      //used for returning timeout
                        checkPoint=true;                                        //break loop
                    }
                }
                signalLength=(int)(Duration.between(beginTime, endTime).toMillis()/minimumSignalLength);    //calculate signal length
                loopStop=true;                                                  //break the main loop
            }
            else
            {
                if((Duration.between(beginTime, Instant.now()).toMillis()/minimumSignalLength)>=timeout)    //timeout for when the signal is not continued
                {
                    timeoutState=true;                                          //used for returning timeout
                    loopStop=true;                                              //break loop
                }
            }
        }
        if(!timeoutState)
        {
            return signalLength;                                                //return signal length, has to be between 1 and 4
        }
        else
        {
            return timeout;                                                     //returns timeout as signal length
        }
    }
    
    /**
     * Method to read incoming data from the SIMON data line as a binary String
     * @author Philipp Schulz
     * @return binary String read from the SIMON data line
     */
    private String readData()
    {
        dataPin.setMode(PinMode.DIGITAL_INPUT);
        dataPin.setPullResistance(PinPullResistance.PULL_UP);                   //configure data line as an input to enable communication
        busyWaitMillis(1);
        String readData = "";                                                   //temporary set the received data
        boolean waitLoop = false;                                               //required boolean value to determine end of sent data
        while(!waitLoop)                                                        //loop for waiting for the enable signal
        {               
            int currentSignalLength = getSignalLength();                        //get the current signal length
            if(currentSignalLength == 3)                                        //if current signal length is enable signal length
            {
                waitLoop = true;                                                //break waiting loop
            }
            else if(currentSignalLength == timeout)                             //if current signal length is timed out
            {
                waitLoop = true;                                                //break waiting loop
            }
        }
        boolean mainLoop = false;                                               //required boolean value to gather data from line and end the transmission
        while(!mainLoop)                                                        //main loop, get every data signal from data line
        {                                  
            int signalLength = getSignalLength();                               //gets the signal length of the current signal sent over DATA line
            if(signalLength == 1)
            {                                                                   //if the read signal length is 1
                readData += "0";                                                //add a 0 to the received data
            }
            else if(signalLength == 2)
            {                                                                   //if the read signal length is 2
                readData += "1";                                                //add a 1 to the received data
            }
            else if((signalLength == 4) || (signalLength == timeout))           //if the read signal length is 4
            {
                mainLoop=true;                                                  //exit the main loop, indicating the end of the transmission
            }
        }
        return readData;                                                        //return the binary String containing the read signals of the DATA line
    }
    
    
    /**
     * Method for sending data over the SIMON protocol
     * @author Philipp Schulz
     * @param receiver_number the device number of the receiving device in the SIMON protocol
     * @param command the String containing the information that should be send to the receiver 
     */
    public synchronized void sendSIMONData(int receiver_number,String command)
    {
        System.out.println("sending...");
        int trialCounter=0;
        boolean confirmation = false;                                           //Boolean value to control the do-while-loop
        do                                                                      //main loop
        {
            String binaryCommand=textToBinary(command);                         //convert command to binary String
            String binaryString=intToBinary(receiver_number)+binaryCommand+getFletcher16Checksums(binaryCommand);//create complete binary data package to send
            sendDataString(binaryString);                                       //send the binary data String
            String readBinaryData = readData();                                 //read the answer
            if("11111111".equals(readBinaryData))                               //if the receiver confirmed transmission
            {
              confirmation=true;                                                //break main loop
            }
            else
            {
                trialCounter++;
            }
            if(trialCounter==5)
            {
                resetSIMON();
                trialCounter=0;
            }
        }while(!confirmation);                                                  //main loop depends on boolean value confirmation
        dataPin.setMode(PinMode.DIGITAL_OUTPUT);                                //set the dataPin as an output
        dataPin.high();                                                         //configure dataPin as high
        busyWaitMillis(5*minimumSignalLength);
        dataPin.setMode(PinMode.DIGITAL_INPUT);
        dataPin.setPullResistance(PinPullResistance.PULL_UP);                   //configure data line as an input to enable communication
        System.out.println("sending done");
    }
    
    /**
     * Method for receiving a String containing data send over the SIMON protocol
     * @author Philipp Schulz
     * @return String of received data as a text
     */
    public synchronized String readSIMONData()
    {
        int trialCounter=0;
        boolean confirmation = false;                                           //Boolean value to control the do-while-loop
        String command="";                                                      //initialize String to return
        do                                                                      //main loop
        {
            String receivedData = readData();                                   //read data from sender
            String receivedBinaryNumber = receivedData.substring(0,8);          //binary receiver number
            int receivedNumber = binaryToInt(receivedBinaryNumber);             //convert binary receiver number to integer
            String textData=receivedData.substring(8,receivedData.length()-16); //get binary text
            command=binaryToText(textData);                                     //convert binary to text
            String receivedChecksums = receivedData.substring(receivedData.length()-16); //convert binary to checksums
            String ownChecksums=getFletcher16Checksums(textData);               //calculate own checksums
            if(receivedChecksums.equals(ownChecksums)&&ownNumber==receivedNumber)//compare received and calculated checksums
            {
                confirmation=true;                                              //break main loop
            }
            else
            {
                trialCounter++;
            }
            if(trialCounter==5)
            {
                resetSIMON();
                trialCounter=0;
            }
        }while(!confirmation);                                                  //main loop depends on boolean value confirmation
        return command;                                                         //return received command as text
    }
    
    /**
     * method for getting the checksums from Fletcher-16 for a given binary data String
     * @param binaryData Binary String for which the checksums should be created
     * @return Checksums in a binary string in the format: "sum1sum2"
     */
    private String getFletcher16Checksums(String binaryData)
    {
        int sumFletcher1=0;                                                     //reset sum1
        int sumFletcher2=0;                                                     //reset sum2
        int length=binaryData.length()/8;                                       //calculate number of blocks of 8
        for(byte i=0;i<length;i++)                                              //loop over data String for each block and calculate the sums
        {
            sumFletcher1=(sumFletcher1+binaryToInt(binaryData.substring(i*8,i*8+8))) % 255; //calculate checksum1
            sumFletcher2=(sumFletcher1+sumFletcher2) % 255;                     //calculate checksum2
        }
        return intToBinary(sumFletcher1)+intToBinary(sumFletcher2);             //return checksums as binary String
    }
    
    
    /**
     * method to determine if a given binary String contains more zeros or ones
     * @author Philipp Schulz
     * @param binaryData Binary String which should be investigated
     * @return True or false, depending if more ones or zeros were in the String
     */
    private boolean zerosOrOnes(String binaryData)                             //method to determine if a given binary String contains more zeros or ones
    {
        int ones=0;                                                             //initialize counter for ones
        int zeros=0;                                                            //initialize counter for zeros
        for(int i=0;i<binaryData.length();i++)                                  //loop over given binary String
        {
            char currentChar = binaryData.charAt(i);                            //get cuttent char
            if(currentChar=='1'){                                               //if current char is 1
                ones++;                                                         //increment counter for ones
            }
            else                                                                //if current char is 0
            {
                zeros++;                                                        //increment counter for zeros
            }
        }
        return ones > zeros; //if the amount of ones is smaller or greater than the number of zeros
    }
}

    /*-------------------multi-purpose pin from stackoverflow---------------------
    // Provisions the pin.
        GpioPinDigitalMultipurpose mypin = gpio.provisionDigitalMultipurposePin(RaspiPin.GPIO_04, PinMode.DIGITAL_INPUT);

    // Sets it as an Output pin.
        mypin.setMode(PinMode.DIGITAL_OUTPUT);

    // Sets the state to "high".
        mypin.high();

    // Sets the state to "low".
        mypin.low();

    // Sets it as an Input pin.
        mypin.setMode(PinMode.DIGITAL_INPUT);
    */
/*--------------------------------------microsecond delay----------------------------------
    //TESTING REQUIRED
    //method for waiting given microseconds, for micros > 10 almost 100% accurate
    //source: http://www.rationaljava.com/2015/10/measuring-microsecond-in-java.html
    public static void busyWaitMicros(long micros){
        long waitUntil = System.nanoTime() + (micros * 1_000);
        while(waitUntil > System.nanoTime()){
            ;
        }
        System.out.println(System.nanoTime());
        System.out.println(waitUntil);
    }
*/
/*----------------------------------measuring time---------------------------------

        //measure time in ms
        Instant now = Instant.now();
        wait(1);
        Duration d = Duration.between(now, Instant.now());
        System.out.println(d.toMillis());
        
        //measure time in ms
        now = Instant.now();
        busyWaitMicros(1000);
        d = Duration.between(now, Instant.now());
        System.out.println(d.toNanos());
        
*/