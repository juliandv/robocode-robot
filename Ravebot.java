//        +---------------------------------------+
//        |                                       |
//        |               RAVEBOT                 |
//        |                                       |
//        |    by Julian de Villiers	          |
//        |                                       |
//        |       26-08-2017   v1.0               |
//        |                                       |
//        +---------------------------------------+

package comp771;

import robocode.*;
import robocode.util.*;
import java.awt.Color;
import java.util.Random;

public class Ravebot extends AdvancedRobot {
    private byte aheadDirection = 1;
    boolean stopTrack = false;
    int trackTime;
    int turnDirection = 1;
    boolean hitState = false;
    int runawayTime;
    Random random = new Random();
    double gameDensity;
    double totalPixels;

    public void run() {

        //control independently
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);

        // Set colors for 'no radar lock' state
        setBodyColor(Color.white);
        setGunColor(Color.white);
        setRadarColor(Color.white);
        setBulletColor(Color.white);
        setScanColor(Color.orange);

        // Gamesize/robot ratio to see how dense the field is with bots
        double gameWidth = getBattleFieldWidth();
        double gameHeight = getBattleFieldHeight();
        totalPixels = gameWidth * gameHeight;
        gameDensity = totalPixels / (getOthers() + 1); // include myself in the equation

        out.println("game density: " + gameDensity);
        if (gameDensity >= 100000) {
            out.println("very spacious!");
        }

        while (true) {

            stopTrack = false;

            if (getOthers() == 1) {
                out.println("lucky last");
            }

            if (hitState == true) {
                quickRunaway();
            } else {
                botScanner();
                setMaxVelocity(3);
                botMovement();

            }
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        if (!stopTrack && !hitState) {

            // http://robowiki.net/wiki/SuperCrazy
            // reduced frequency to prevent it getting too crazy
            if (e.getTime() % 2 == 0) {
                setBodyColor(new Color((float) Math.random(), (float) Math.random(), (float) Math.random()));
                setGunColor(Color.red);
                setRadarColor(new Color((float) Math.random(), (float) Math.random(), (float) Math.random()));
                setBulletColor(new Color((float) Math.random(), (float) Math.random(), (float) Math.random()));
            }

            trackTime = trackTime + 1;
            if (trackTime > 50 && getOthers() > 1) {
                stopTrack = true;
                trackTime = 0;
                out.println("Take a look around");
            }


            // Noniterative Linear Targeting
            //Sourced from http://robowiki.net/wiki/Linear_Targeting
            double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
            setTurnGunRightRadians(Utils.normalRelativeAngle(absoluteBearing -
                    getGunHeadingRadians() + (e.getVelocity() * Math.sin(e.getHeadingRadians() -
                    absoluteBearing) / 13.0)));

//          Fire when the gun is ready
            if (getGunHeat() == 0) {
                setFire(4);
            }

            execute();

            // Radar lock - Sourced from
            // http://old.robowiki.net/robowiki?Radar
            // absolute bearing
            double angle = e.getBearingRadians() + getHeadingRadians();
            // normalize the radar angle and iden arc to prevent slip
            double rAngle = Utils.normalRelativeAngle(angle - getRadarHeadingRadians());
            rAngle = (rAngle > 0) ? .17 : -.17;

            setTurnRadarRightRadians(rAngle);
            execute();

            setMaxVelocity(3);
            botMovement();

        }
    }

    public void botScanner() {
        // Set colors
        setBodyColor(Color.white);
        setGunColor(Color.white);
        setRadarColor(Color.white);
        setBulletColor(Color.white);
        setScanColor(Color.orange);

        setTurnRadarRightRadians(Double.POSITIVE_INFINITY); // move radar infinitely
        scan();
    }
    public void quickRunaway() {
        out.println("ouch!");
        botScanner();

        // Set the runaway speed based on the amount
        // of room we have to move

        // First recalculate the game density based on current players
        gameDensity = totalPixels / (getOthers() + 1);

        double runawaySpeed = 0;
        if (gameDensity > 80000) {
            runawaySpeed = 8;
            out.println("spacious. Use speed of 8");
        } else if (gameDensity > 30000 && gameDensity <= 80000) {
            runawaySpeed = 7;
            out.println("medium. Use speed of 7");
        } else if (gameDensity <= 2000) {
            runawaySpeed = 5;
            out.println("dense. Use speed of 5");
        }

        // now do a small runaway!

        setBodyColor(Color.red);
        setGunColor(Color.red);
        setRadarColor(Color.red);

        setMaxVelocity(runawaySpeed);
        botMovement();

        // Keep moving each tick
        // Length of movement time is based on runaway speed
        runawayTime++;
        if (runawayTime > (runawaySpeed * 2)) {
            hitState = false;
            runawayTime = 0;
        }
    }

    public void botMovement() {

        // dont hit any walls unnecessarily!
        double xPos = getX();
        double yPos = getY();
        double wallBuffer = 5;
        
        if (       xPos + wallBuffer > getBattleFieldWidth()
                || yPos + wallBuffer > getBattleFieldHeight()
                || xPos - wallBuffer < 0 || yPos - wallBuffer < 0) {
            aheadDirection *= -1;
        }

        // in case of bot collision, or failed wall detection
        if (getVelocity() == 0) {
            aheadDirection *= -1;
        }

        int turnAmount = 5;
        if (getTime() % 20 == 0) { 
            
            turnDirection = -turnDirection;

            // add a bit of randomness ot the turn amount (from 2 - 100)
            // https://stackoverflow.com/questions/2444019/how-do-i-generate-a-random-integer-between-min-and-max-in-java
            turnAmount = random.nextInt(100 + 1 - 2) + 2;
        }
        setTurnLeft(turnAmount * turnDirection);
        // Start moving (and turning)
        setAhead(100 * aheadDirection);
        execute();
        
        trackTime = 0; // reset track counter
    }

    public void onHitByBullet(HitByBulletEvent event) {
        hitState = true;
    }
}