package template;

import java.util.HashSet;
import java.util.Set;
import logist.task.Task;

public class TreeNode {
	static int counter = 0;
	int index = 0;
	int[] location;
	double g_score = Double.MAX_VALUE;
	double f_score = Double.MAX_VALUE;
	Set<Task> carried_tasks = new HashSet<Task>();
	
	public TreeNode(int[] current_location){
		index = counter;
		this.location = current_location;
		counter++;
	}
}
