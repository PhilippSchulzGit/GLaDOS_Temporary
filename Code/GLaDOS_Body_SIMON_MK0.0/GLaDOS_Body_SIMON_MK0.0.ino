/*
 * Code for handling all features of the body of GLaDOS
 * Receiving Commands over the SIMON MK0.0 protocol
 */
#include <Wire.h>
#include <Adafruit_PWMServoDriver.h>
Adafruit_PWMServoDriver pwm = Adafruit_PWMServoDriver();              //object of the pwm driver board
#define FREQUENCY 60      //frequency of PWM board
#define LEDS 8           //IO pin for controlling the LEDs of GLaDOS
#define EYE 9            //IO pin for controlling the eye of GLaDOS
//servo constants
int currentHead = 0;      //current position of the head servo
int currentNeck = 0;      //current position of the neck servo
int currentBody = 0;      //current position of the body servo
int currentCenter = 0;    //current position of the center servo
int homeHead = 70;        //home position of the head servo
int homeNeck = 90;        //home position of the neck servo
int homeBody = 150;       //home position of the body servo
int homeCenter = 90;      //home position of the center servo

int positionsHead[]={30,38,46,54,62,70,78,86,94,102,110};
int positionsNeck[]={65,69,72,76,79,83,86,90,93,97,100};
int positionsBody[]={120,126,132,138,144,150,156,162,168,174,180};
int positionsCenter[]={-20,2,24,56,68,90,112,134,156,178,200};

//--------------------------------------------------SIMON---------------------------------------------------------------------------------
//SIMON constants
#define DATA 10                                       //Data line for the SIMON protocol
#define SIGNAL 11                                     //Signal line for the SIMON protocol
#define OWN_NUMBER 2                                  //device number of the device this code should run on
#define MINIMUM_SIGNAL_LENGTH 1                       //time in ms that is the minimum signal length of the SIMON protocol
#define TIMEOUT 200                                   //time in ms that will pass until the method getSignalLength() times out
//----------------------------------------Variables for protocol---------------------------
String readBinaryData="000000100011000100100000001100011000001000000101";
String receivedBinaryNumber="00000010";
int receivedNumber=0;
String textData="001100010010000000110001";
String command="1 1";
String receivedChecksums="1000001000000101";
String ownChecksums="1000001000000101";
String confirmationString="11111111";
//----------------------------------------Variables for protocol---------------------------
//SIMON functions
void begin_SIMON(int data_pin, int signal_pin) {      //function to set up the pins for the SIMON protocol
  pinMode(data_pin, INPUT_PULLUP);                    //set DATA line as output
  pinMode(signal_pin, OUTPUT);                        //set SIGNAL line as output
  digitalWrite(signal_pin,LOW);                       //signal line must stay as a low output for establishing a common ground
  Serial.begin(57600);                                //temp, for debugging
}

String textToBinary(String text) {                    //function for converting a String containing text into a String containing binary
  String binaryText="";                               //String that wil contain the binary representation of the text
  for(int i=0; i<text.length(); i++){                 //main loop, go over every char and convert it into 8bit binary
   char currentChar = text.charAt(i);                 //get cuttent char
   for(int i=7; i>=0; i--){                           //iterate over all its of char, from msb to lsb (bit loop)
      byte bytes = bitRead(currentChar,i);            //read current bit
      if(bytes==0) {                                  //decide what to add to binary String
        binaryText+="0";
      } else {
        binaryText+="1";
      }
    }                                                 //end bit loop
  }                                                   //end main loop
  return binaryText;                                  //return binary representation of text as a String
}

String binaryToText(String binary) {                  //function for converting a String containing binary into a String containing text
  String text="";                                     //initialize the String containing the result text
  byte countBit=7;                                    //variable required to go over all individual bits of a byte
  byte countByte=0;                                   //variable required to keep track of which byte is currently written to
  int charSize=binary.length()/8;                     //get the required size for a byte array
  byte allChars[charSize];                            //initialize a new byte array with a fitting size
  for(int i=0;i<binary.length();i++) {                //main loop, going over every binary digit
      if(binary.charAt(i)=='1') {                     //check which digit is currently given in the specified position of the String
        bitSet(allChars[countByte],countBit);         //set the corresponding bit in the byte array to true
      } else {
        bitClear(allChars[countByte],countBit);       //set the corresponding bit in the byte array to false
      }
      if(countBit>0) {                                //required to change the count variables of the bit and byte
        countBit--;                                   //decrement countBit if greater than 0
      } else {
        countBit=7;                                   //set countBit to 7 if equal to or smaller than 0
        countByte++;                                  //increment countByte by 1 for the next byte
      }
  }
  for(int i=0;i<charSize;i++) {                       //loop over entire byte array to construct result String
    char singleChar = allChars[i];                    //get the current byte of the array and cast it to a char
    text+=String(singleChar);                         //add the current char to the result String
  }
  return text;                                        //return text converted from binary
}

String intToBinary(int receiver_number) {             //convert integer to binary String
  return textToBinary(String((char)receiver_number)); //use function textToBinary to convert the integer into a binary String
}

int binaryToInt(String binary) {                      //convert binary to integer
  char s[8];                                          //char array that will be used to convert the binary String to an integer
  binary.toCharArray(s, 9);                           //convert the String into a char array
  return strtol(s, (char**) NULL, 2);                 //return an integer got from the char array
}

void binaryNumberConversion(){
  char s[8];                                          //char array that will be used to convert the binary String to an integer
  receivedBinaryNumber.toCharArray(s, 9);                           //convert the String into a char array
  receivedNumber= strtol(s, (char**) NULL, 2);                 //return an integer got from the char array
}

void sendDataString(String binaryString) {            //sending the contents of a String containing binary data including begin and end signal
  pinMode(DATA,OUTPUT);                               //set the DATA pin as an output
  digitalWrite(DATA,HIGH);                            //end enable signal
  delay(2*MINIMUM_SIGNAL_LENGTH);
  for(int i=0;i<8;i++){                               //instead of waiting for 16*minimumSignalLength, toggle DATA line
    digitalWrite(DATA,LOW);                           //change DATA pin to LOW
    delay(MINIMUM_SIGNAL_LENGTH);                     //delay for minimumSignalLength
    digitalWrite(DATA,HIGH);                          //change DATA pin to HIGH
    delay(MINIMUM_SIGNAL_LENGTH);                     //delay for minimumSignalLength
  }
  digitalWrite(DATA,LOW);                             //begin enable signal
  delay(3*MINIMUM_SIGNAL_LENGTH);                     //wait the time for enable signal
  digitalWrite(DATA,HIGH);                            //end enable signal
  delay(MINIMUM_SIGNAL_LENGTH);
  for(int i=0;i<binaryString.length();i++) {          //loop over binary String
    char current=binaryString.charAt(i);              //get current char from binary string
    digitalWrite(DATA,LOW);                           //begin signal binary String digit
    if(current=='0') {                                //find out if current char is 0 or 1
      delay(MINIMUM_SIGNAL_LENGTH);                   //if 0, wait for minimum signal length
    } else {
      delay(2*MINIMUM_SIGNAL_LENGTH);                 //if 1, wait for 2*minimum signal length
    }
    digitalWrite(DATA,HIGH);                          //end of single binary String digit
    delay(MINIMUM_SIGNAL_LENGTH);                     //Wait minimum signal length
  }
  digitalWrite(DATA,LOW);                             //begin disable signal
  delay(4*MINIMUM_SIGNAL_LENGTH);                     //wait the time for disable signal
  digitalWrite(DATA,HIGH);                            //end disable signal
  delay(2);                                           //wait until receiver device notices last digital level
}

byte getSignalLength() {                              //determines the length of the current signal on the signal line, function blocks when called
  bool loopStop=0;                                    //required boolean value for main loop
  byte signalLength=0;                                //declare value to return, used in later calculation
  bool timeout=0;
  while(!loopStop) {                                  //main loop, determines signal length
    if(!digitalRead(DATA)) {                          //if signal is LOW
      unsigned long beginTime=micros();               //save current timestamp
      unsigned long endTime=0;                        //save current timestamp
      bool checkPoint=0;                              //variable to end while loop
      while(!checkPoint) {                            //when signal is still LOW
        if(digitalRead(DATA)){                        //if signal ends (e.g. goes from low to high)
          checkPoint=1;                               //break main loop
          endTime=micros();                           //save current time to determine signal length
        }
        if((micros()-beginTime)/(1000*MINIMUM_SIGNAL_LENGTH)>=TIMEOUT){  //timeout for when the signal is not continued
          timeout=1;                                  //used for returning timeout
          checkPoint=1;                               //break loop
        }
      }
      signalLength=(endTime-beginTime)/(1000*MINIMUM_SIGNAL_LENGTH);   //calculate signal length, middle factor based on micros() (1000) or millis() (1)
      loopStop=1;                                         //break the main loop
    }
  }
  if(!timeout){
    return signalLength;                              //return signal length, has to be between 1 and 4
  }else{
    return TIMEOUT;                                   //returns timeout as signal length
  }
}

void readData() {                                   //reads and returns incoming data from the SIMON data lines as a binary String (from enable signal to disable signal)
  pinMode(DATA,INPUT_PULLUP);                         //configure data line as an input to enable other communication
  delay(1);
  readBinaryData="";                                 //temporary set the received data
  bool waitLoop=0;                                    //required boolean value to determine end of sent data
  while(!waitLoop) {                                  //loop for waiting for the enable signal
    int currentSignalLength=getSignalLength();        //get the current signal length
    if(currentSignalLength==3) {                      //if current signal length is enable signal length
      waitLoop=1;                                     //break waiting loop
    }else if(currentSignalLength==TIMEOUT){           //if current signal length is timed out
      waitLoop=1;
    }
  }
  bool mainLoop=0;                                    //required boolean value to gather data from line and end the transmission
  while(!mainLoop) {                                  //main loop, get every data signal from data line
    byte signalLength=getSignalLength();              //gets the signal length of the current signal sent over DATA line
    if(signalLength==1) {                             //if the read signal length is 1
      readBinaryData+="0";                                  //add a 0 to the received data
    } else if(signalLength==2) {                      //if the read signal length is 2
      readBinaryData+="1";                                  //add a 1 to the received data
    } else if(signalLength==4||signalLength==TIMEOUT) {  //if the read signal length is 4
      mainLoop=1;                                     //exit the main loop, indicating the end of the transmission
    }
  }
  //return readData;                                    //return the binary String containing the read signals of the DATA line
}

void sendSIMONData(int receiver_number,String command){
  bool confirmation=0;
  do{
    String binaryCommand=textToBinary(command);
    String binaryString=intToBinary(receiver_number)+binaryCommand+getFletcher16Checksums(binaryCommand);
    sendDataString(binaryString);

    readData();
    if(confirmationString.equals(readBinaryData)){
      confirmation=1;
    }
  }while(!confirmation);
}

String readSIMONData(){
  bool confirmation=0;
  do{
    Serial.println("n");
    readData();
    Serial.println(readBinaryData);
    receivedBinaryNumber = readBinaryData.substring(0,8);//binary receiver number
    Serial.println(receivedBinaryNumber);
    binaryNumberConversion();
    Serial.println(receivedNumber); //this print seems to be required, whatever the reason is
    textData=readBinaryData.substring(8,readBinaryData.length()-16); //get binary text
    command=binaryToText(textData);            //convert binary to text
    Serial.println(command);
    receivedChecksums = readBinaryData.substring(readBinaryData.length()-16); //convert binary to checksums
    ownChecksums=getFletcher16Checksums(textData);
    if(receivedChecksums.equals(ownChecksums)&&receivedNumber==OWN_NUMBER){
      sendDataString(confirmationString);
      confirmation=1;
    }
  }while(!confirmation);
  return command;
}

// method for getting the checksums from Fletcher-16 for a given binary data String
// returns the Checksums in a binary string in the format: "sum1sum2"
String getFletcher16Checksums(String binaryData) {
  byte sumFletcher1=0;                                //reset sum1
  byte sumFletcher2=0;                                //reset sum2
  byte length=binaryData.length()/8;                  //calculate number of blocks of 8
  for(byte i=0;i<length;i++) {                        //loop over data String for each block and calculate the sums
    sumFletcher1=(sumFletcher1+binaryToInt(binaryData.substring(i*8,i*8+8))) % 255; //calculate checksum1
    sumFletcher2=(sumFletcher1+sumFletcher2) % 255;   //calculate checksum2
  }
  return intToBinary(sumFletcher1)+intToBinary(sumFletcher2); //return checksums as binary String
}

bool zerosOrOnes(String binaryData){                  //method to determine if a given binary String contains more zeros or ones
  int ones=0;                                         //initialize counter for ones
  int zeros=0;                                        //initialize counter for zeros
  for(int i=0;i<binaryData.length();i++){             //loop over given binary String
    char currentChar = binaryData.charAt(i);          //get cuttent char
    if(currentChar=='1'){                             //if current char is 1
      ones++;                                         //increment counter for ones
    }else{                                            //if current char is 0
      zeros++;                                        //increment counter for zeros
    }
  }
  if(ones<=zeros){                                    //if the amount of ones is smaller than or equal to the number of zeros
    return 0;
  }else{                                              //if the amount of ones is greater than the number of zeros
    return 1;
  }
}
//--------------------------------------------------SIMON---------------------------------------------------------------------------------

//--------------------------------------------------MAIN PROGRAM--------------------------------------------------------------------------
int pulseWidth(int angle){                                            //helper function for converting servo angles into 4096 bit values
  int pulse_wide, analog_value;
  pulse_wide = map(angle, 0, 180, 650, 2350);
  analog_value = int(float(pulse_wide) / 1000000 * FREQUENCY * 4096);
  return analog_value;
}

void setEye(bool eyeState) {                                          //function to enable and disable the eye of the body
  digitalWrite(EYE,eyeState);
}

void setLEDs(bool ledState) {                                         //function to enable and disable the LEDs of the body
  digitalWrite(LEDS,ledState);
}

void setServos(int servoHead, int servoNeck, int servoBody, int servoCenter) {    //function to set the servos to desired locations, stepping 1 degree at a time
  bool check=0;                                                       //initialize while-loop variable as false
  turnServosOffOrOn(1);                                               //turn pwm controller on to change servo position
  while(!check) {                                                     //loop until check is true
    //handling the position of the head
    if(currentHead!=servoHead){
      if(currentHead<servoHead){
        currentHead+=1;
      }else{
        currentHead-=1;
      }
      pwm.setPWM(15,0,pulseWidth(currentHead));
    }
    //handling the position of the neck
    if(currentNeck!=servoNeck){
      if(currentNeck<servoNeck){
        currentNeck+=1;
      }else{
        currentNeck-=1;
      }
      pwm.setPWM(13,0,pulseWidth(currentNeck));
    }
    //handling the position of the body
    if(currentBody!=servoBody){
      if(currentBody<servoBody){
        currentBody+=1;
      }else{
        currentBody-=1;
      }
      pwm.setPWM(14,0,pulseWidth(currentBody));
    }
    //handling the position of the center
    if(currentCenter!=servoCenter){
      if(currentCenter<servoCenter){
        currentCenter+=1;
      }else{
        currentCenter-=1;
      }
      pwm.setPWM(12,0,pulseWidth(currentCenter));
    }
    //check if position is reached
    if(currentHead==servoHead&&currentNeck==servoNeck&&currentBody==servoBody&&currentCenter==servoCenter){
      check=1;                                                      //set check to true, breaking the loop in next iteration
    }
    delay(10);                                                      //pause the servo movement for a smoother movement overall
  }
  turnServosOffOrOn(0);                                             //turn off servo controller to save power and reduce noise
}

void homeServos() {                                                 //function for homing the servos, resulting in a fast movement to the defined home position of all servos
  turnServosOffOrOn(1);                                               //turn pwm controller on to change servo position
  pwm.setPWM(15,0,pulseWidth(homeHead)); //Head
  pwm.setPWM(13,0,pulseWidth(homeNeck));//Neck
  pwm.setPWM(14,0,pulseWidth(homeBody)); //Body
  pwm.setPWM(12,0,pulseWidth(homeCenter));//Center
  currentHead = homeHead;
  currentNeck = homeNeck;
  currentBody = homeBody;
  currentCenter = homeCenter;
  turnServosOffOrOn(0);                                             //turn off servo controller to save power and reduce noise
}

void turnServosOffOrOn(bool input) {                               //turns servos off or on, depending on input, TRUE for turning on, FALSE for tunring off
  if(input) {
    pwm.wakeup();
  } else {
    pwm.sleep();
  }
}

void inputHandler() {                                               //handles different commands sent via SIMON accordingly to their content
  String input = readSIMONData();                                   //get the command via SIMON, blocks until command is received
  Serial.println(input);
  /*input can have different lengths:
   * 1. size=3, for controlling LEDs, eye and on/off servos
   * -> Format of input string: "mode state", where mode={1,2,3} and state={0,1}
   * 2. size>>3, for controlling servos
   * -> Format of input string: "mode servo1, servo2, servo3, servo4", where mode=4 and servo1={0...9},servo2={0...9},servo3={0...9},servo1={0...9}
   * min: "4 0 0 0 0"
   * max: "4 10 10 10 10"
   * */
   int controlMode=getValue(input,' ',0).toInt();                   //get the first char of String, used as mode control
   byte inputLength=input.length();                                 //length of total sent command
   if(inputLength==3) {                                             //controlMode 1,2 and 3, handling states of eye, LEDs and Servos (on/off)
    //control of LEDs and eye
    int controlState=getValue(input,' ',1).toInt();
    if(controlMode==1) {
      //control eye
      setEye(controlState);
    } else if (controlMode==2) {
      //control LEDs
      setLEDs(controlState);
    } else if (controlMode==3) {
      //control on/off servos
      turnServosOffOrOn(controlState);
    }
   } else if((inputLength==9)&&controlMode==4) {               //controlMode 4, handling servo positions
    //control of servos
    turnServosOffOrOn(1);
    int newServo1=getValue(input,' ',1).toInt();
    int newServo2=getValue(input,' ',2).toInt();
    int newServo3=getValue(input,' ',3).toInt();
    int newServo4=getValue(input,' ',4).toInt();
    setServos(positionsHead[newServo1], positionsNeck[newServo2], positionsBody[newServo3], positionsCenter[newServo4]);
    turnServosOffOrOn(0);
   }
}

//from: https://arduino.stackexchange.com/questions/1013/how-do-i-split-an-incoming-string (10.05.2020, 17:28)
/*input arguments:
 * data :     String which should be separated
 * separator: Char which acts as the separator
 * index:     index as the occurence of the seperator in the data String# 
 * example:   data="4 110 100 180 200",separator=' ',index=0 returns "4", index=1 returns "110", index=2 returns "100" etc
 */
String getValue(String data, char separator, int index)
{
    int found = 0;
    int strIndex[] = { 0, -1 };
    int maxIndex = data.length() - 1;

    for (int i = 0; i <= maxIndex && found <= index; i++) {
        if (data.charAt(i) == separator || i == maxIndex) {
            found++;
            strIndex[0] = strIndex[1] + 1;
            strIndex[1] = (i == maxIndex) ? i+1 : i;
        }
    }
    return found > index ? data.substring(strIndex[0], strIndex[1]) : "";
}

void setup() {
  //initialize SIMON protocol
  begin_SIMON(DATA,SIGNAL);
  //set up pwm board
  pwm.begin();
  pwm.setPWMFreq(FREQUENCY);
  //set up LEDs and the eye as outputs, pulling them low
  pinMode(EYE,OUTPUT);
  pinMode(LEDS,OUTPUT);
  digitalWrite(EYE,0);
  digitalWrite(LEDS,0);
  //set all servos into a defined state (the movement will be instantanious and fast)
  homeServos();
}

void loop() {
  /*functionality of this program:
   * 1. Switch Eye LED on and off     -> setEye(bool eyeState)
   * 2. Switch LEDs on and off        -> setLEDs(bool ledState)
   * 3. set servos to given location  -> setServos(int servoHead, int servoNeck, int servoBody, int servoCenter)
   * 4. turn all servos on and off    -> turnServosOffOrOn(bool input)
   * */
  inputHandler();
}
