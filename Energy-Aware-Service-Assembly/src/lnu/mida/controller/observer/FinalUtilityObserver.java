package lnu.mida.controller.observer;

import java.io.PrintStream;
import java.util.ArrayList;

import lnu.mida.controller.init.OverloadFileInitializer;
import peersim.config.Configuration;
import peersim.core.*;
import peersim.util.*;

/**
 * I am an observer that prints, at each timestep, the minimum, average and
 * maximum utility of all fully resolved services.
 */
public class FinalUtilityObserver implements Control {

	// ///////////////////////////////////////////////////////////////////////
	// Fields
	// ///////////////////////////////////////////////////////////////////////

	/**
	 * The name of this observer in the configuration file. Initialized by the
	 * constructor parameter.
	 */
	private final String name;

	// Quality
	public static ArrayList<IncrementalStats> quality;	
	public static ArrayList<IncrementalStats> quality_jain;
	
	
	// Energy	
	public static ArrayList<IncrementalStats> energy;
	public static ArrayList<IncrementalStats> energy_jain;
	
	// Network	
	public static ArrayList<IncrementalStats> networkSize;
	
	// Availability	
	public static ArrayList<IncrementalStats> availability;

	// numero medio di cicli in cui un nodo è attivo
	public static int total_up;
	//numero medio di cicli in cui un nodo è inattivo
	public static int total_down;
	
	//numero di cicli in cui tutti i nodi sono attivi
	public static int t_all_cycles;
	//numero di cicli in cui almeno un nodo è inattivo
	public static int d_one_cycles;
	
	// ///////////////////////////////////////////////////////////////////////
	// Constructor
	// ///////////////////////////////////////////////////////////////////////

	/**
	 * Standard constructor that reads the configuration parameters. Invoked by
	 * the simulation engine.
	 * 
	 * @param name
	 *            the configuration prefix for this class.
	 */
	public FinalUtilityObserver(String name) {
		this.name = name;
	}

	// ///////////////////////////////////////////////////////////////////////
	// Methods
	// ///////////////////////////////////////////////////////////////////////

	@Override
	public boolean execute() {
		
		int exp_number = OverloadFileInitializer.experiment_number;
		int total_exps = Configuration.getInt("simulation.experiments",1);
		
		// print data from last experiment
		if(exp_number==total_exps) {
			
			System.out.println("********** total up *********");
			System.out.println("\n         " + total_up/total_exps);
			
			System.out.println("********** total down *********");
			System.out.println("\n         " + total_down/total_exps);

			System.out.println("\n\n\n\n");
			System.out.println("********** t_all_cycles *********");
			System.out.println("\n         " + t_all_cycles/total_exps);

			System.out.println("********** d_one_cycles *********");
			System.out.println("\n         " + d_one_cycles/total_exps);

			
			
			PrintStream ps = OverloadFileInitializer.getPs_final();
			
			int n = 1;
			for (IncrementalStats incrementalStats : quality.subList(1, quality.size() ) ) {
					
				int index = quality.indexOf(incrementalStats);
				
				// Quality
				double finalQuality = incrementalStats.getAverage();
				IncrementalStats quality_jain_is = quality_jain.get(index);		
				double finalQualityFairness = quality_jain_is.getAverage();
				
				// Energy
				IncrementalStats energy_is = energy.get(index);	
				IncrementalStats energy_jain_is = energy_jain.get(index);	
				
				double finalEnergy = energy_is.getAverage();
				double finalEnergyFairness = energy_jain_is.getAverage();
				
				// Network
				IncrementalStats nodesAlive_is = networkSize.get(index);	
				double nodesAlive = nodesAlive_is.getAverage();
				
				// Availability
				IncrementalStats avail_is = availability.get(index);	
				double availability = avail_is.getAverage();

				
				ps.print(n+" ");
				ps.print(finalQuality+" ");
				ps.print(finalQualityFairness+" ");
				ps.print(finalEnergy+" ");
				ps.print(finalEnergyFairness+" ");
				ps.print(nodesAlive+" ");
				ps.print(availability+"\n");
				
				n+=1; // learning step
			}						
		}
		
		return false;
	}	
	
}
