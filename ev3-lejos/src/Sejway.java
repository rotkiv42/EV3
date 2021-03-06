import lejos.hardware.Button;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.NXTMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3GyroSensor;
import lejos.robotics.EncoderMotor;

//import lejos.nxt.*;

/**
 * A controller for a self-balancing Lego robot with a light sensor
 * on port 2. The two motors should be connected to port B and C.
 *
 * Building instructions in Brian Bagnall: Maximum Lego NXTBuilding 
 * Robots with Java Brains</a>, Chapter 11, 243 - 284
 * 
 * @author Brian Bagnall
 * @version 26-2-13 by Ole Caprani for leJOS version 0.9.1
 */

public class Sejway 
{
    // PID constants from others
//    final float KP = 1.556f;
//    final float KI = 0.222f;
//    final float KD = 1.833f;

//  final float KP = 1.7f;
//  final float KI = 0.222f;
//  final float KD = 2.3f;

	// (1.5, 0.01, 0) working in oscillation
	// (1.5, 0.01, 10) working more stable
	// (1.5, 0.01, 20) working more stable with 1 sample
	// (10, 0.1, 23) working very stable with 5 samples
    final float KP = 10f; // 1.5f working, 5 better, 1 bit slow, 3/10 good, 15/20 too fast, default 28
    final float KI = 0.1f; // 0.01 working, 0.00001/0.01 better, 0.001/0.1 good, 0.5/1 too fast, default 4, depends on sample time dt
    final float KD = 23f; // 0/10 working, 0.001/0.01/0.1 good, 1 too fast, default 33
    // PID constants
//	kp = 0.0336f;
//	ki = 0.2688f;
//	kd = 0.000504f;
//	private static final float Kp = 0.5f;  // default 0.5f
//	private static final float Ki = 11;   // default 11
//	private static final float Kd = 0.005f; // default 0.005f
//    final int SCALE = 1;  // default 18
    final int base_power = 20; // 30 bit fast, 10 not moving, default 20 good

//    LightSensor ls;
    EV3GyroSensor gyro = new EV3GyroSensor(SensorPort.S2);
//    private DifferentialPilot pilot = new DifferentialPilot(5.6, 9.25, Motor.A, Motor.D);
	EncoderMotor leftMotor = new NXTMotor(MotorPort.A); 
	EncoderMotor rightMotor = new NXTMotor(MotorPort.D); 
	
	// Global vars:
	float offset = 0;
	float prev_error;
	float int_error;
	
	public static void main(String[] args) 
	{
		Sejway sej = new Sejway();
		sej.start();
	}
	
    public void start() {
//        ls = new LightSensor(SensorPort.S2, true);
    	gyro.reset();
    	for(int i = 0; i < 20; i ++) {
    		offset += gyroRate();
    	}
    	offset /= 20f;
    	
//		sej.getBalancePos();
    	pidControl();
    	shutDown();
    	
    	try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }
    
    float gyroRate() {
    	return gyroRate(1);
    }
    
    /**
	 * average of samples of angular velocity
	 */
    float gyroRate(int sample_size) {
		float filter = 0;

		// get samples
		float[] sample = new float[1];
		for(int i = 0; i < sample_size; i ++) {
			gyro.getRateMode().fetchSample(sample, 0);
			filter += sample[0];
//			filter = ev3.getAngularVelocity () + filter;
		}
		
		return filter / sample_size;
	}
	
    public void pidControl() 
    {
    	System.out.println("PID control");
        while (!Button.ESCAPE.isDown()) 
        {
//            int normVal = ls.readNormalizedValue();
        	float normVal = gyroRate(5);  // [-440, 440]
//        	float normVal = gyroRate();

            // Proportional Error:
            float error = normVal - offset;
            // Adjust far and near light readings:
//            if (error < 0) error = (int)(error * 1.8F);
			
            // Integral Error:
//            int_error = ((int_error + error) * 2)/3;
            int_error = (int_error + error);
			
            // Derivative Error:
            float deriv_error = error - prev_error;
            prev_error = error;
			
//            int pid_val = (int)(KP * error + KI * int_error + KD * deriv_error) / SCALE;
            int pid_val = (int)(KP * error + KI * int_error + KD * deriv_error);
			
            // may need to change to check if outbound count > 20
            if (pid_val > 100)
                pid_val = 100;
            if (pid_val < -100)
                pid_val = -100;

            // Power derived from PID value:
            int power = Math.abs(pid_val);
//            power = 55 + (power * 45) / 100; // Default NORMALIZE POWER 55 + => [55,100]
            power = base_power + (power * (100 - base_power)) / 100; // [10,100]
//            System.out.println(normVal + " " + pid_val + " " + power);
            
            int sign = -1 * (int) Math.signum(pid_val);
            leftMotor.setPower(sign*power);
            rightMotor.setPower(sign*power);
            /*
//            if (pid_val >= 0) {
            if (pid_val < 0) {
//                MotorPort.B.controlMotor(power, BasicMotorPort.FORWARD);
//                MotorPort.C.controlMotor(power, BasicMotorPort.FORWARD);
            	leftMotor.forward();
            	rightMotor.forward();
            } else {
//                MotorPort.B.controlMotor(power, BasicMotorPort.BACKWARD);
//                MotorPort.C.controlMotor(power, BasicMotorPort.BACKWARD);
            	leftMotor.backward();
            	rightMotor.backward();
            }*/
        }
        System.out.println("complete");
    }
	
    public void shutDown()
    {
        // Shut down light sensor, motors
//        Motor.B.flt();
//        Motor.C.flt();
//        ls.setFloodlight(false);
    	leftMotor.flt();
    	rightMotor.flt();
    }
    
    public void getBalancePos() 
    {
    	// Wait for user to balance and press orange button
    	while (!Button.ENTER.isDown())
    	{
    		// NXTway must be balanced.
//    		offset = ls.readNormalizedValue();
    		LCD.clear();
//    		LCD.drawInt(offset, 2, 4);
    		LCD.refresh();
    	}
    }
}
