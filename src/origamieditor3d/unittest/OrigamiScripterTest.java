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

}
