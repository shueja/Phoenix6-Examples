// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.sim.CANcoderSimState;

import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.simulation.DCMotorSim;
import edu.wpi.first.wpilibj.smartdashboard.Mechanism2d;
import edu.wpi.first.wpilibj.smartdashboard.MechanismLigament2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj.util.Color8Bit;

/**
 * The VM is configured to automatically run this class, and to call the functions corresponding to
 * each mode, as described in the TimedRobot documentation. If you change the name of this class or
 * the package after creating this project, you must also update the build.gradle file in the
 * project.
 */
public class Robot extends TimedRobot {
  private final double PRINT_PERIOD = 0.5; // Update every 500 ms

  private final CANcoder cancoder = new CANcoder(1, "rio");
  private double currentTime = Timer.getFPGATimestamp();
  
  /* Sim only */
  private final double HEIGHT = 1;
  private final double WIDTH = 1;
  private final double ROOT_X = WIDTH / 2;
  private final double ROOT_Y = HEIGHT / 2;

  private final CANcoderSimState cancoderSim = cancoder.getSimState(); // We need a sim state in order to change the values of CANcoder
  private final DCMotorSim motorSim = new DCMotorSim(DCMotor.getFalcon500(1), 100, .001);
  private final XboxController controller = new XboxController(0); // Uses an Xbox controller for setting the CANcoder simulation
  private Mechanism2d mech = new Mechanism2d(WIDTH, HEIGHT); // Main mechanism object
  private MechanismLigament2d wrist = mech.
                                      getRoot("base", ROOT_X, ROOT_Y).
                                      append(new MechanismLigament2d("Wrist", .25, 90, 6, new Color8Bit(Color.kAliceBlue)));

  private MechanismLigament2d leftArrow = wrist.append(new MechanismLigament2d("LeftArrow", 0.1, 150, 6, new Color8Bit(Color.kAliceBlue)));
  private MechanismLigament2d rightArrow = wrist.append(new MechanismLigament2d("RightArrow", 0.1, -150, 6, new Color8Bit(Color.kAliceBlue)));
  /* End sim only */

  /**
   * This function is run when the robot is first started up and should be used for any
   * initialization code.
   */
  @Override
  public void robotInit() {
    /* Configure CANcoder */
    var toApply = new CANcoderConfiguration();

    /* User can change the configs if they want, or leave it empty for factory-default */

    cancoder.getConfigurator().apply(toApply);

    /* Speed up signals to an appropriate rate */
    cancoder.getPosition().setUpdateFrequency(100);
    cancoder.getVelocity().setUpdateFrequency(100);
  }

  @Override
  public void robotPeriodic() {
    if (Timer.getFPGATimestamp() - currentTime > PRINT_PERIOD) {
      currentTime += PRINT_PERIOD;

      /**
       * getPosition automatically calls refresh(), no need to manually refresh.
       * 
       * StatusSignalValues also have the toString method implemented, to provide
       * a useful print of the signal.
       */
      var pos = cancoder.getPosition();
      System.out.println("Position is " + pos.toString() + " with " + pos.getTimestamp().getLatency() + " seconds of latency");

      /**
       * Get the velocity StatusSignalValue
       */
      var vel = cancoder.getVelocity();
      /* This time wait for the signal to reduce latency */
      vel.waitForUpdate(PRINT_PERIOD); // Wait up to our period
      /**
       * This uses the explicit getValue and getUnits functions to print, even though it's not
       * necessary for the ostream print
       */
      System.out.println("Velocity is " +
                         vel.getValue() + " " +
                         vel.getUnits() + " with " +
                         vel.getTimestamp().getLatency() + " seconds of latency");
      /**
       * Notice when running this example that the second print's latency is always shorter than the first print's latency.
       * This is because we explicitly wait for the signal using the waitForUpdate() method instead of using the refresh()
       * method, which only gets the last cached value (similar to how Phoenix v5 works).
       * This can be used to make sure we synchronously update our control loop from the CAN bus, reducing any latency or jitter in
       * CAN bus measurements.
       * When the device is on a CANivore, the reported latency is very close to the true latency of the sensor, as the CANivore
       * timestamps when it receives the frame. This can be further used for latency compensation.
       */
      System.out.println();
    }
  }

  @Override
  public void autonomousInit() {}

  @Override
  public void autonomousPeriodic() {}

  @Override
  public void teleopInit() {
    /**
     * When we teleop init, set the position of the Pigeon2 and wait for the setter to take affect.
     */
    cancoder.setPosition(0.4, 0.1); // Set our position to .4 rotations and wait up to 100 ms for the setter to take affect
    cancoder.getPosition().waitForUpdate(0.1); // And wait up to 100 ms for the position to take affect
    System.out.println("Set the position to 0.4 rotations, we are currently at " + cancoder.getPosition()); // Use java's implicit toString operator
  }

  @Override
  public void teleopPeriodic() {}

  @Override
  public void disabledInit() {}

  @Override
  public void disabledPeriodic() {}

  @Override
  public void testInit() {}

  @Override
  public void testPeriodic() {}

  @Override
  public void simulationInit() {
  }

  @Override
  public void simulationPeriodic() {
    double Yaxis = controller.getLeftY();
    double motorVoltage = Yaxis * 12; // scales joystick axcis to motor voltage ( +-12v)
    motorSim.setInputVoltage(motorVoltage);
    motorSim.update(.02);
    double position = motorSim.getAngularPositionRotations();
    double velocity = motorSim.getAngularVelocityRPM()/60;
    cancoderSim.setRawPosition(position);
    cancoderSim.setVelocity(velocity);

    SmartDashboard.putData("mech2d", mech);
    
    wrist.setAngle(position*360); //converts 1 rotation to 360 degrees
  }
}
