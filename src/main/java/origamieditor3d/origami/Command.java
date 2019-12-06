package origamieditor3d.origami;

public interface Command {
	

	/**
	 * Execute the command.
	 */
	void execute();
	
	/**
	 * Execute the opposite command.
	 */
	void undo();
	
	double [] getPpoint();
	double [] getPnormal();


}
