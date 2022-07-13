#include <Wire.h>
#include <math.h>
#include <Arduino.h> 

#define GYR_ADDRESS (0xd2>>1) 


#define GYRO_XOUT_L   0x0C
#define GYRO_XOUT_H   0x0D  
#define GYRO_YOUT_L   0x0E
#define GYRO_YOUT_H   0x0F
#define GYRO_ZOUT_L   0x10
#define GYRO_ZOUT_H   0x11
#define CMD      0x7E 

class BMI160
{
  public:
    typedef struct vector
    {
      int16_t x, y, z;
    } vector;
    vector g; // gyro angular velocity readings

    void enableDefault(void);
    void writeReg(byte reg, byte value);
    void read(void);
};

void BMI160::enableDefault(void)
{
   
    writeReg(CMD, 0x15);

    writeReg(0x40,0x29);
    writeReg(0x42,0x29);
}

void BMI160::writeReg(byte reg, byte value)
{
  Wire.beginTransmission(GYR_ADDRESS);
  Wire.write(reg);
  Wire.write(value);
  Wire.endTransmission();
}
void BMI160::read()
{

  Wire.beginTransmission(GYR_ADDRESS);
  Wire.write(GYRO_XOUT_L);
  Wire.requestFrom(GYR_ADDRESS, 6,true); 
  Wire.endTransmission(true);
  //Serial.println(Wire.available());
 while (Wire.available() < 6)
 {
      Wire.beginTransmission(GYR_ADDRESS);
      Wire.write(GYRO_XOUT_L);
      Wire.requestFrom(GYR_ADDRESS, 6,true); 
      Wire.endTransmission(true);
  }
    uint8_t xla = Wire.read();
    uint8_t xha = Wire.read();
    uint8_t yla = Wire.read();
    uint8_t yha = Wire.read();
    uint8_t zla = Wire.read();
    uint8_t zha = Wire.read();
    g.x = xha << 8 | xla;
    g.y = yha << 8 | yla;
    g.z = zha << 8 | zla;

}


BMI160 gyro;

unsigned long starttime=0;
unsigned long stoptime;
unsigned long looptime;
//char text[1024];
void setup() {
  //memset(text,0,sizeof(text));
  Serial.begin(115200);
  Wire.begin();
  gyro.enableDefault();

}

void loop(){
//  starttime=micros();
  
  /*
  Serial.print((int)gyro.g.x*0.007629,5);
  Serial.print("   ");
  Serial.print((int)gyro.g.y*0.007629,5);
  Serial.print("   ");
  Serial.println((int)gyro.g.z*0.007629,5);
  */
  
  char text[32];
  double x,y,z;
  double acc_x,acc_y,acc_z;
  gyro.read();
  x=(int)gyro.g.x*0.061037;//0.007629;
  y=(int)gyro.g.y*0.061037;//0.007629;
  z=(int)gyro.g.z*0.061037;//0.007629;
//  delay(1000);
  delayMicroseconds(7600);
  Serial.print(x); 
  Serial.print("  ");
  Serial.print(y); 
  Serial.print("  ");
  Serial.print(z);
  stoptime=micros();
  Serial.print("  ");
  Serial.println(stoptime-starttime);
  starttime=stoptime;
//  stoptime=micros();
//  looptime=stoptime-starttime;
//  Serial.println(looptime);
  
 
}
