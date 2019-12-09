package origamieditor3d.origami;

public class CommandFoldCrease extends CommandFold {
	static public final int foldID = 5;

	protected int phi;

	public CommandFoldCrease(double[] ppoint, double[] pnormal, OrigamiGen1 origami) {
		super(origami);
		this.ppoint = ppoint;
		this.pnormal = pnormal;
	}

	@Override
	public void execute() {
		origami.internalRotationFold(ppoint, pnormal, 0);
	}

	@Override
	public void undo() {}
	
	@Override  
	public int getPhi() {
		return this.phi;
	}
}

