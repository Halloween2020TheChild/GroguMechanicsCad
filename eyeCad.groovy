import com.neuronrobotics.bowlerstudio.creature.ICadGenerator
import com.neuronrobotics.sdk.addons.kinematics.AbstractLink
import com.neuronrobotics.sdk.addons.kinematics.DHLink
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics
import com.neuronrobotics.sdk.addons.kinematics.LinkConfiguration
import com.neuronrobotics.sdk.addons.kinematics.MobileBase

import eu.mihosoft.vrl.v3d.CSG
import eu.mihosoft.vrl.v3d.Cube
import eu.mihosoft.vrl.v3d.Sphere
import javafx.scene.paint.Color
import javafx.scene.transform.Affine

//Your code here

return new ICadGenerator() {

	private MobileBase arg0;

	@Override
	public ArrayList<CSG> generateCad(DHParameterKinematics d, int linkIndex) {
		CSG headBall = new Sphere(2*25.4/2.0).toCSG()	

		ArrayList<DHLink> dhLinks = d.getChain().getLinks()
		DHLink dh = dhLinks.get(linkIndex)
		// Hardware to engineering units configuration
		LinkConfiguration  conf = d.getLinkConfiguration(linkIndex);
		// Engineering units to kinematics link (limits and hardware type abstraction)
		AbstractLink abstractLink = d.getAbstractLink(linkIndex);
		// Transform used by the UI to render the location of the object
		
		Affine manipulator = dh.getListener();
		Affine root=d.getRootListener()
		println "Root Affine ID="+root
		CSG headBallTop =headBall.intersect(headBall.getBoundingBox().toYMin())
		
		CSG jaw =headBall.intersect(headBall.getBoundingBox().toYMax().toXMin())
		
		jaw.setManipulator(manipulator)
		headBallTop.setManipulator(manipulator)
		jaw.setColor(Color.WHITE)
		
		def list= Arrays.asList(headBallTop,jaw) ;
		for(CSG c:list) {
			c.setManufacturing({return null})
		}
	}

	@Override
	public ArrayList<CSG> generateBody(MobileBase arg0) {
		this.arg0 = arg0;
		// TODO Auto-generated method stub
		
		return Arrays.asList(new Cube(1).toCSG()) ;
	}
	
}