package origamieditor3d.origami;

import java.util.LinkedList;

public class CommandsList {

	/** The commands. */
	private LinkedList<CommandFold> commands;
	
	/** The index. */
	private int index;
	
	
	/** The window. */

	/**
	 * Instantiates a new CommandsList.
	 * @param window the window
	 */
	public CommandsList() {

		commands = new LinkedList<CommandFold>();
		index = -1;
	}
	
	
	/**
	 * Adds a command.
	 *
	 * @param command the command to add
	 */
	public void addCommand(CommandFold command, boolean execute) {
		int i = index+1;
		while(i<commands.size()) {
			commands.remove(i);
		}
		index++;
		commands.add(command);
		if (execute) {
			command.execute();
		}
	}
	
	
	/**
	 * Undo the command.
	 */
	public void undo() {
		if(index >= 0)
		{
			commands.get(index).undo();
			index--;
		} 
	
	}
	
	/**
	 * Redo the command.
	 */
	public void redo() {
		if(index<commands.size()-1)
		{
			index++;
			commands.get(index).execute();
		}
	
	}
	
	/**
	 * Reset the list.
	 */
	public void reset() {
		index = -1;
		commands.clear();
	
	}
	
	public void redoAll() {
		if(index<commands.size()-1)
		{
			index++;
			this.redo();
		}
	
	}
	
	public int getSize() {
		return commands.size();
	}
	
	
	public void executeAtIndex(int index) {
		commands.get(index).execute();
	}
	public CommandFold getAtIndex(int index) {
		return commands.get(index);
	}
	public void clearPart (int start, int end) {
		commands.subList(start, end).clear();
	}



}
