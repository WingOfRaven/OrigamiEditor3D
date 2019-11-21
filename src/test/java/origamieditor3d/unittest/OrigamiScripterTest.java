package origamieditor3d.unittest;
import junit.framework.*;
import origamieditor3d.script.OrigamiScripter;

public class OrigamiScripterTest extends TestCase{


	public void testpaperOneArg() throws Exception {
		assertEquals("paper [test] ",OrigamiScripter.paper("test"));
	}
	public void testpaperFourArgs() throws Exception {
		assertEquals("paper [1.0 2.0 3.0 4.0] ",OrigamiScripter.paper(1.0,2.0,3.0,4.0));
	}
	public void testCorner() throws Exception {
		assertEquals("corner [1.0 2.0] ",OrigamiScripter.corner(1.0,2.0));
	}
	public void testTarget() throws Exception {
		assertEquals("target [1.0 2.0] ",OrigamiScripter.target(1.0,2.0));
	}
	public void testAngle() throws Exception {
		assertEquals("angle 1 ",OrigamiScripter.angle(1));
	}
	
	public void testPlane() throws Exception {
		assertEquals("plane [1.0 2.0] [3.0 4.0 5.0] ",OrigamiScripter.plane(new double[] {1.0,2.0}, new double[] {3.0, 4.0, 5.0}));
	}
	
	public void testPlanePoint() throws Exception {
		assertEquals("planepoint [1.0 2.0 3.0] ",OrigamiScripter.planepoint(new double[] {1.0,2.0, 3.0}));
	}
	public void testPlaneNormal() throws Exception {
		assertEquals("planenormal [1.0 2.0 3.0] ",OrigamiScripter.planenormal(new double[] {1.0,2.0, 3.0}));
	}
	
	public void testPlaneThrough() throws Exception {
		assertEquals("planethrough [1.0 2.0 3.0] [4.0 5.0 6.0] [7.0 8.0 9.0] ",OrigamiScripter.planethrough(new double[] {1.0,2.0, 3.0}, new double[] {4.0,5.0, 6.0}, new double[] {7.0,8.0, 9.0}));
	}
	public void testAngleBisector() throws Exception {
		assertEquals("angle-bisector [1.0 2.0 3.0] [4.0 5.0 6.0] [7.0 8.0 9.0] ",OrigamiScripter.angle_bisector(new double[] {1.0,2.0, 3.0}, new double[] {4.0,5.0, 6.0}, new double[] {7.0,8.0, 9.0}));
	}
	public void testCamera() throws Exception {
		assertEquals("camera [1.0 2.0 3.0] [4.0 5.0 6.0] [7.0 8.0 9.0] ",OrigamiScripter.camera(new double[] {1.0,2.0, 3.0}, new double[] {4.0,5.0, 6.0}, new double[] {7.0,8.0, 9.0}));
	}
	
	public void testFilename() throws Exception {
		assertEquals("filename [test] ",OrigamiScripter.filename("test"));
	}
	
	public void testTitle() throws Exception {
		assertEquals("title [test] ",OrigamiScripter.title("test"));
	}
	
	public void testColor() throws Exception {
		assertEquals("color 12 ",OrigamiScripter.color(12));
		
	}
	

}
