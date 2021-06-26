//temporary Arduino Nano program for GLaDOS functioning as a normal lamp
#define LAMP 8
#define EYE 9
void setup() {
  pinMode(LAMP, OUTPUT);
  pinMode(EYE, OUTPUT);
  digitalWrite(LAMP,HIGH);
  digitalWrite(EYE,HIGH);
}

void loop() {
  delay(1000);
}
