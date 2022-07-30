import com.neuronrobotics.bowlerstudio.creature.MobileBaseLoader
import com.neuronrobotics.bowlerstudio.physics.TransformFactory
import com.neuronrobotics.bowlerstudio.vitamins.Vitamins
import com.neuronrobotics.sdk.addons.kinematics.AbstractLink
import com.neuronrobotics.sdk.addons.kinematics.DHLink
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics
import com.neuronrobotics.sdk.addons.kinematics.LinkConfiguration
import com.neuronrobotics.sdk.addons.kinematics.MobileBase
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR
import com.neuronrobotics.sdk.common.DeviceManager

import eu.mihosoft.vrl.v3d.CSG
import eu.mihosoft.vrl.v3d.Cylinder
import eu.mihosoft.vrl.v3d.RoundedCube
import eu.mihosoft.vrl.v3d.RoundedCylinder
import eu.mihosoft.vrl.v3d.Transform
import javafx.scene.paint.Color
import javafx.scene.transform.Affine

CSG moveDHValues(CSG incoming,DHLink dh ){
	TransformNR step = new TransformNR(dh.DhStep(0)).inverse()
	Transform move = com.neuronrobotics.bowlerstudio.physics.TransformFactory.nrToCSG(step)
	return incoming.transformed(move)
}

if(args==null) {
	HashMap<String,Object> measurmentsMotor = Vitamins.getConfiguration(  "LewanSoulMotor","lx_224")
	double motorz =  measurmentsMotor.body_z
	double centerTheMotorsValue=motorz/2;
	MobileBase base=DeviceManager.getSpecificDevice( "Standard6dof",{
		//If the device does not exist, prompt for the connection
		
		MobileBase m = MobileBaseLoader.fromGit(
			"https://github.com/Halloween2020TheChild/GroguMechanicsCad.git",
			"hephaestus.xml"
			)
		return m
	})
	def motorLocation = new TransformNR(0,0,centerTheMotorsValue,new RotationNR())
	def d =base.getAllDHChains().get(0)
	motorLocation=new TransformNR(0,0,d.getDH_D(5)-centerTheMotorsValue,new RotationNR(0,0,0))
					.times(motorLocation
						.times(new TransformNR(0,0,d.getDH_D(4),new RotationNR(0,-90,0))))
	
	args = [d,4,centerTheMotorsValue,motorLocation]
}


int linkIndex = args[1]
DHParameterKinematics d= args[0];
ArrayList<DHLink> dhLinks = d.getChain().getLinks()
DHLink dh = dhLinks.get(linkIndex)
// Hardware to engineering units configuration
LinkConfiguration  conf = d.getLinkConfiguration(linkIndex);
// Engineering units to kinematics link (limits and hardware type abstraction)
AbstractLink abstractLink = d.getAbstractLink(linkIndex);
// Transform used by the UI to render the location of the object

//Horn section
Affine manipulator = dh.getListener();
def type=	d.getLinkConfiguration(linkIndex-1).getShaftType()
def size = d.getLinkConfiguration(linkIndex-1).getShaftSize()
CSG vitaminCad=   Vitamins.get(	type,size)
.movez(args[2])
def HornModel=moveDHValues(vitaminCad,dh)
HornModel.setManipulator(manipulator)

//END horn

//Bearing 
HashMap<String, Object> hornCOnfig = Vitamins.getConfiguration(type,size)
def mountPlateToHornTop = hornCOnfig.get("mountPlateToHornTop")
def bearingHeight =mountPlateToHornTop-2 +d.getDH_D(linkIndex+1)
CSG thrust = Vitamins.get("ballBearing","Thrust_1andAHalfinch")
						.movez(bearingHeight)
thrust.setManipulator(manipulator)
//END bearing


TransformNR motorLocation=args[3]

//Motor for next link
CSG motor=   Vitamins.get(conf.getElectroMechanicalType(),conf.getElectroMechanicalSize())
def motorModel = motor.transformed(TransformFactory.nrToCSG(motorLocation))
motorModel.setManipulator(manipulator)

// Keepaway for motor of this link
HashMap<String, Object> motormeasurments = Vitamins.getConfiguration(conf.getElectroMechanicalType(),conf.getElectroMechanicalSize())
def b_y = motormeasurments.body_x/2
def hyp =Math.sqrt(b_y*b_y+b_y*b_y)
def centerTobottom = args[2]+motormeasurments.shoulderHeight
def centerToTop=args[2]+mountPlateToHornTop
def kwCanheight =args[2]+centerTobottom
def linkageThicknessSMallShaftLen = motormeasurments.bottomShaftLength

CSG keepawayCan = new Cylinder(hyp+1, kwCanheight+1).toCSG()
					.toZMax()
					.movez(args[2]+1)
CSG shaftKW = new Cylinder(motormeasurments.bottomShaftDiameter/2, kwCanheight+linkageThicknessSMallShaftLen*4).toCSG()
				.toZMax()
				.movez(args[2])			
keepawayCan=moveDHValues(keepawayCan.union(shaftKW),dh)
keepawayCan.setManipulator(manipulator)
// END motor keepaway

//Horn keepaway
CSG hornkw = new Cylinder(hornCOnfig.hornDiameter/2+1, mountPlateToHornTop+1).toCSG()
			.movez(d.getDH_D(linkIndex+1))
hornkw.setManipulator(manipulator)
// end horn keepaway

// Hull Bulding BLocks
def cornerRad=2
double baseCorRad = Vitamins.getConfiguration("ballBearing","Thrust_1andAHalfinch").outerDiameter/2+5
double linkThickness = baseCorRad-centerToTop
double linkYDimention = motormeasurments.body_x;
CSG linkBuildingBlockRoundCyl = new Cylinder(linkYDimention/2,linkYDimention/2,linkThickness,30)
.toCSG()
CSG linkBuildingBlockRoundSqu = new RoundedCube(linkYDimention,linkYDimention,linkThickness)
.cornerRadius(cornerRad)
.toCSG()
.toZMin()
CSG linkBuildingBlockRound = new RoundedCylinder(linkYDimention/2,linkThickness)
.cornerRadius(cornerRad)
.toCSG()
//END building blocks

//Drive Side
def backsetBoltOne = -linkYDimention/2-5
def backsetBoltTwo=-25.0/2
CSG shaftMount = linkBuildingBlockRound
					.movez(centerToTop)
CSG nutsert = moveDHValues(
				Vitamins.get("heatedThreadedInsert", "M5")
				.toZMax().movez(centerToTop),dh)
				.movez(backsetBoltOne)
				.movex(-backsetBoltTwo/2)
				.movez(d.getDH_D(linkIndex+1))
CSG nutsert2=nutsert.movex(backsetBoltTwo)

CSG bolt = moveDHValues(
	Vitamins.get("capScrew", "M5")
	.movez(centerToTop+linkThickness),dh)
	.movez(backsetBoltOne)
	.movex(-backsetBoltTwo/2)
	.movez(d.getDH_D(linkIndex+1)).setManipulator(manipulator)
CSG bolt2=bolt.movex(backsetBoltTwo)
.setManipulator(manipulator)
CSG driveSide = moveDHValues(shaftMount,dh)
CSG driveConnector = driveSide.movez(d.getDH_D(linkIndex+1))
						.movez(backsetBoltOne)
						.movex(-backsetBoltTwo/2)
CSG driveBolt2=driveConnector.movex(backsetBoltTwo)

CSG driveUnit=driveConnector
					.union(driveSide)
					.union(driveBolt2)
					.hull()
driveSide=driveSide.union(driveUnit)
				.difference(HornModel)
				.difference([bolt,bolt2])
driveSide.setManipulator(manipulator)
//END Drive side

//PassiveSIde
def passiveTHickness = baseCorRad-centerTobottom
println "Link thickness = "+linkThickness+" passive side = "+passiveTHickness
CSG passiveMount = new RoundedCylinder(linkYDimention/2,passiveTHickness)
					.cornerRadius(cornerRad)
					.toCSG()
					.movez(-baseCorRad)

CSG passiveSide = moveDHValues(passiveMount,dh)
CSG passivConnector = passiveSide.movez(d.getDH_D(linkIndex+1))
						.movez(backsetBoltOne)
						.movex(-backsetBoltTwo/2)
CSG passivBolt2=passivConnector.movex(backsetBoltTwo)

CSG passiveUnit=	passivConnector
						.union(passiveSide)
						.union(passivBolt2)
						.hull()

							
passiveSide=passiveSide.union(passiveUnit)
					
passiveSide.setManipulator(manipulator)
//End Passive Side

double servoSupport =8
//Servo mount
def supportBeam= new RoundedCube(baseCorRad*2.0-cornerRad,motormeasurments.body_y+linkThickness*2+servoSupport,25)
					.cornerRadius(cornerRad).toCSG()
					.toZMax()
					.toYMin()
					.movey(-baseCorRad-servoSupport)
					.movex(cornerRad/2)
					.transformed(TransformFactory.nrToCSG(motorLocation))
//END Servo Mount

// Bearing Mount
def baseCoreheight = vitaminCad.getTotalZ()-mountPlateToHornTop

CSG baseCore = new Cylinder(baseCorRad,baseCorRad,baseCoreheight-0.25,36).toCSG()
				.toZMax()
				.movez(mountPlateToHornTop-0.25)
				.transformed(TransformFactory.nrToCSG(motorLocation))
				.union(supportBeam)
				.hull()
				.union(passiveSide)
				.difference(thrust)
				.difference(motorModel)
				.difference(hornkw)
				.difference([bolt,bolt2,nutsert,nutsert2,driveSide,keepawayCan])
				.setManipulator(manipulator)
//END Bearing Mount

driveSide.setManufacturing({
	it.rotx(-90).toZMin()
})
baseCore.setManufacturing({
	it.rotx(-90).toZMin()
})
driveSide.setName("wrist2Drive")
baseCore.setName("wrist2ThrustBearingSide")

return [driveSide,baseCore].collect{it.setColor(javafx.scene.paint.Color.RED)}
