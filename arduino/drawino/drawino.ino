
#include <Stepper.h>
#include <Servo.h>

#define STEP_PER_REV 800
#define STAEP_SPEED 20
#define PERIMETER 4.5 //change this
#define LENGTH_PER_STEP ((1/STEP_PER_REV)*PERIMETER)

Stepper stepperL(STEP_PER_REV, 5, 6);
Stepper stepperR(STEP_PER_REV, 9, 10);
Servo myservo;

float x_stepperL ; // x coordinate of steper left
float x_stepperR ; // x coordinate of steper right
float y_steppers ; // y coordinate of stepers
float lengthL ; // length of thread left
float lengthR ; // length of thread right
float old_lengthL ; // old length of thread left
float old_lengthR ; // old length of thread right
float length_per_step; // the length of the thread that the motor moves for each step

float total_steps_L ;
float total_steps_R ;
  
bool x_coordinate;
String string_x;
String string_y;
char command;

void setup() {
  Serial.begin(9600);
  myservo.attach(3);
  
  Serial.println('!');

  x_stepperL = 0; // x coordinate of steper left
  x_stepperR = 69; // x coordinate of steper right
  y_steppers = 52; // y coordinate of stepers
  lengthL = 36; // length of thread left
  lengthR = 36; // length of thread right
  old_lengthL = 36; // old length of thread left
  old_lengthR = 36; // old length of thread right
  length_per_step = 0.005 ; // the length of the thread that the motor moves for each step
  x_coordinate = true;
  string_x = "";
  string_y = "";

  touchUp();
  // set the speed of the motor to 100 RPMs
  stepperL.setSpeed(STAEP_SPEED);
  stepperR.setSpeed(STAEP_SPEED);

}

void loop() {

  while (Serial.available() > 0)
  {

    command = Serial.read();  //gets one byte from serial buffer
   // Serial.println(command);
    if (command == '-') {
      touchStart();
      Serial.println('!');
    }
    else if (command == '_') {
      touchUp();
      Serial.println('!');
    }

    else if (command == '@') {

      rotateSteppersSimultaneously(string_x.toDouble(), y_steppers - string_y.toDouble());
      Serial.println('!');
      x_coordinate = true;
      string_x = "";
      string_y = "";
    }

    else if (command == ',') {
      x_coordinate = false;
    }

    //else if(x_coordinate && isDigit(command)){
    else if (x_coordinate && (isDigit(command) || command == '.') ) {
      string_x += command;
    }

    else if (!x_coordinate && (isDigit(command) || command == '.') ) {
      string_y += command;
    }
  }
}

/*
   Calculate the length of the thread that needs to reach point p
   @param x : x coordinate of point P
   @param y : y coordinate of point P
*/
bool newLength(float x, float y) {
  old_lengthL = lengthL;
  old_lengthR = lengthR;

  float disL = sqrt( pow(x_stepperL - x, 2) + pow(y_steppers - y, 2) ) ;
  float disR = sqrt( pow(x_stepperR - x, 2) + pow(y_steppers - y, 2) ) ;

  total_steps_L = (disL - old_lengthL) / length_per_step;
  total_steps_R = (disR - old_lengthR) / length_per_step;

  if( abs(total_steps_L) < 1 && abs(total_steps_R) < 1 )
    return false ;
    
  lengthL = disL ;
  lengthR = disR ;

  return true;
}


/*
   Rotate stepers simultaneously to point P
   @param x : x coordinate of point P
   @param y : y coordinate of point P
*/

void rotateSteppersSimultaneously(float x, float y) {
        
  if(!newLength(x, y)){
    return;
  }
  
  //calculate the amount of steps and speed for each single progress in the while loop
  int step_L ;
  int step_R ;
  
  if ( (abs(total_steps_L) >= 1) && (abs(total_steps_R) > abs(total_steps_L)) ) {
    step_L = total_steps_L / abs(total_steps_L) ;
    step_R = total_steps_R / abs(total_steps_L);
  }
  else if( (abs(total_steps_R) >= 1) && (abs(total_steps_R) <= abs(total_steps_L)) ) {
    step_L = total_steps_L / abs(total_steps_R) ;
    step_R = total_steps_R / abs(total_steps_R);
  }
  else if( abs(total_steps_L) < 1 ){
    step_L = 0 ;
    step_R = total_steps_R / abs(total_steps_R);   
  }
  else{
    step_L = total_steps_L / abs(total_steps_L) ;
    step_R = 0 ;     
  }

  //rotate stepers simultaneously
  int i = 0;
 
  if(step_L == 0)
    stepperR.step(total_steps_R);
  else if(step_R == 0)
    stepperL.step(total_steps_L);
    
  else{
    while ( i * abs(step_L) < abs(total_steps_L) || i * abs(step_R) < abs(total_steps_R) ) {
      if (i * abs(step_L) < abs(total_steps_L)) {
        stepperL.step(step_L);
      }
      if (i * abs(step_R) < abs(total_steps_R)) {
        stepperR.step(step_R);
      }
      i++;
    }
  }

}



void touchStart() {
  int pos = 0;
  for (; pos <= 15; pos += 1) {
    // in steps of 1 degree
    myservo.write(pos);
    delay(30);
  }
}
void touchUp() {
  int pos = 15;
  for (; pos >= 0; pos -= 1) {
    // in steps of 1 degree
    myservo.write(pos);
    delay(30);
  }
}
