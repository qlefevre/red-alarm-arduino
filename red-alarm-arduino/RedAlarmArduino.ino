/*
  RedAlarmArduino
  
 Turns on the LED when "ON" is sent on the serial port, turn off the led when "OFF" is sent.
 
 This example code is in the public domain.
 
 */

// Pin 13 has an LED connected on most Arduino boards.
// give it a name:
int led = 13;
int led2 = 2;
int led3 = 3;
int led4 = 4;
int led5 = 5;
String cmd;
#include <SPI.h>


// the setup routine runs once when you press reset:
void setup() {                
  // initialize the digital pin as an output.
  pinMode(led, OUTPUT);     
  pinMode(led2, OUTPUT);  
  pinMode(led3, OUTPUT);  
  pinMode(led4, OUTPUT);  
  pinMode(led5, OUTPUT);
  digitalWrite(led, LOW);    // turn the LED off by making the voltage LOW
  digitalWrite(led2, HIGH);    // turn the LED off by making the voltage LOW
  Serial.begin(9600);
  Serial.println("START");
  Serial.println("HELLO RED ALARM ARDUINO");
}

// the loop routine runs over and over again forever:
void loop() {

  // Read String 
  cmd = Serial.readStringUntil('\n');


  // Check String 
  if(String("on").equalsIgnoreCase(cmd)){
    Serial.println("ON");
    digitalWrite(led, HIGH);   // turn the LED on (HIGH is the voltage level)
    digitalWrite(led2, LOW);    // turn the LED off by making the voltage LOW
    digitalWrite(led3, LOW);    // turn the LED off by making the voltage LOW
    digitalWrite(led4, LOW);    // turn the LED off by making the voltage LOW
    digitalWrite(led5, LOW);    // turn the LED off by making the voltage LOW
  }
  else if(String("off").equalsIgnoreCase(cmd)){
    Serial.println("OFF");
    digitalWrite(led, LOW);    // turn the LED off by making the voltage LOW
    digitalWrite(led2, HIGH);    // turn the LED off by making the voltage LOW
    digitalWrite(led3, HIGH);    // turn the LED off by making the voltage LOW
    digitalWrite(led4, HIGH);    // turn the LED off by making the voltage LOW
    digitalWrite(led5, HIGH);    // turn the LED off by making the voltage LOW
  }

}


