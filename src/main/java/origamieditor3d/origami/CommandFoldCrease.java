package origamieditor3d.origami;

public class CommandFoldCrease extends CommandFold {
	
	int phi;
	static {
		foldId = 5;
	}
	public CommandFoldCrease(double[] ppoint, double[] pnormal, OrigamiGen1 origami) {

		this.ppoint = ppoint;
		this.pnormal = pnormal;
		this.origami = origami;

	}
	
	public void execute() {
		origami.internalRotationFold(ppoint, pnormal, 0);
	}
	public void undo() {}
	
	@Override  
	public int getPhi() {
		return this.phi;
	}


}

