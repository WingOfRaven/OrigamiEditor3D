package origamieditor3d.origami;

public class CommandFoldCrease extends CommandFold {
	
	int phi;
	public CommandFoldCrease(double[] ppoint, double[] pnormal, int phi, OrigamiGen1 origami) {

		this.ppoint = ppoint;
		this.pnormal = pnormal;
		this.phi = phi;
		this.origami = origami;

	}
	
	public void execute() {
		origami.internalRotationFold(ppoint, pnormal, 0);
	}
	public void undo() {}


}

