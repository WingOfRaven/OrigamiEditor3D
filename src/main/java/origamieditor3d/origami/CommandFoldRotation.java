package origamieditor3d.origami;

public class CommandFoldRotation extends CommandFold {
	static public final int foldID = 2;

	int phi;

	public CommandFoldRotation(double[] ppoint, double[] pnormal, int phi, OrigamiGen1 origami) {
		super(origami);
		this.ppoint = ppoint;
		this.pnormal = pnormal;
		this.phi = phi;
	}

	@Override
	public void execute() {
		origami.internalRotationFold(ppoint, pnormal, phi);
	}

	@Override
	public void undo() {}
	
	@Override
	public int getPhi() {
		return this.phi;
	}
}

