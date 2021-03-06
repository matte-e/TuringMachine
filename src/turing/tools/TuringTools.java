package turing.tools;

import bn.blaszczyk.rosecommon.RoseException;
import bn.blaszczyk.rosecommon.controller.ModelController;
import bn.blaszczyk.roseservice.web.HtmlBuilder;
import turing.model.Direction;
import turing.model.State;
import turing.model.Status;
import turing.model.Step;
import turing.model.TapeCell;
import turing.model.TuringMachine;
import turing.model.Value;

public class TuringTools
{

	
	private static final int SHOW_CELLS = 5;

	public static void step(final ModelController controller, final TuringMachine machine) throws RoseException
	{
		final Status status = machine.getStatus();
		if(!status.isRunning())
			return;
		final State state = status.getCurrentState();
		final TapeCell cell = status.getCurrentCell();
		final Value value = cell.getValue();
		for(final Step step : state.getStepTos())
			if(step.getReadValue().equals(value))
			{
				final State nextState = step.getStateTo();
				final Value writeValue = step.getWriteValue();
				final boolean directionRight = step.getDirection().equals(Direction.RIGHT);
				TapeCell nextCell = directionRight ? cell.getNext() : cell.getPrevious();
				if(nextCell == null)
				{
					nextCell = controller.createNew(TapeCell.class);
					if(directionRight)
					{
						cell.setNext(nextCell);
						nextCell.setPrevious(cell);
					}
					else
					{
						cell.setPrevious(nextCell);
						nextCell.setNext(cell);
					}
				}
				cell.setValue(writeValue);
				status.setCurrentCell(nextCell);
				status.setCurrentState(nextState);
				nextCell.setStatus(status);
				nextState.getStatuss().add(status);
				cell.setStatus(null);
				state.getStatuss().remove(status);
				controller.update(status, cell, nextCell, state, nextState);
				return;
			}
		status.setRunning(false);
		controller.update(status);
	}

	public static String createWebPage(final TuringMachine machine) 
	{
		final HtmlBuilder builder = new HtmlBuilder();
		
		final Status status = machine.getStatus();
		builder.h1("Turing Machine: " + machine.getName());
		builder.h2("Program: " + machine.getProgram().getName());
		builder.append("running: " + status.isRunning());
		
		builder.h2("Tape:");
		TapeCell showCell = status.getCurrentCell();
		for(int i = 0; i < SHOW_CELLS; i++)
		{
			if(showCell.getPrevious() == null)
				break;
			showCell = showCell.getPrevious();
		}
		if(showCell.getPrevious() != null)
			builder.append("... - ");
		while(showCell != status.getCurrentCell())
		{
			builder.append(showCell.getValue() + " - " );
			showCell = showCell.getNext();
		}
		builder.append("<b>" + showCell.getValue() + "</b>");
		for(int i = 0; i < SHOW_CELLS; i++)
		{
			showCell = showCell.getNext();
			if(showCell == null)
				break;
			builder.append( " - " + showCell.getValue());
		}
		if(showCell != null && showCell.getNext() != null)
			builder.append(" - ...");
		
		final State state = status.getCurrentState();
		builder.h2("Current State: " + state.getName());
		builder.append("Possible next states:<br>");
		builder.append("<table>");
		builder.append("<tr><th>read value</th><th>write value</th><th>tape direction</th><th>state name</th></tr>");
		for(final Step step : state.getStepTos())
			builder.append(String.format("<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>",step.getReadValue(), step.getWriteValue(), step.getDirection(), step.getStateTo().getName()));
		builder.append("</table>");
		
		builder.append("<form method=\"post\" action=\"/turing/" + machine.getId() + "/step\">");
		builder.append("<input type=\"submit\" value=\"step\" />");
		builder.append("</form>");
		return builder.build();
	}

}
