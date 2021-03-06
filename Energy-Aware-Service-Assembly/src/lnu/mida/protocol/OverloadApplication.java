package lnu.mida.protocol;

import java.util.ArrayList;
import java.util.LinkedList;
import com.lajv.location.Location;
import lnu.mida.entity.EnergyBatteryPanelReputation;
import lnu.mida.entity.EnergyPanelReputation;
import lnu.mida.entity.GeneralNode;
import lnu.mida.entity.GreenReputation;
import lnu.mida.entity.QOSReputation;
import lnu.mida.entity.Service;
import peersim.cdsim.CDProtocol;
import peersim.cdsim.CDState;
import peersim.config.Configuration;
import peersim.core.Cleanable;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;

public class OverloadApplication implements CDProtocol, Cleanable {

	// ///////////////////////////////////////////////////////////////////////
	// Constants
	// ///////////////////////////////////////////////////////////////////////

	/**
	 * The component assambly protocol.
	 */
	private static final String COMP_PROT = "comp_prot";

	/**
	 * The strategy
	 */
	private static String STRATEGY = "";

	// ///////////////////////////////////////////////////////////////////////
	// Fields
	// ///////////////////////////////////////////////////////////////////////

	/**
	 * The component assembly protocol id.
	 */
	private final int component_assembly_pid;

	/** The learner's container */
	private ArrayList<QOSReputation> qosReputations;
	private ArrayList<EnergyBatteryPanelReputation> energyBPReputations;
	private ArrayList<EnergyPanelReputation> energyPReputations;
	private ArrayList<GreenReputation> greenReputations;

	/**
	 * Initialize this object by reading configuration parameters.
	 * 
	 * @param prefix the configuration prefix for this class.
	 */
	public OverloadApplication(String prefix) {
		super();
		STRATEGY = Configuration.getString("STRATEGY", "no strat");
		component_assembly_pid = Configuration.getPid(prefix + "." + COMP_PROT);
		qosReputations = new ArrayList<QOSReputation>();
		energyBPReputations = new ArrayList<EnergyBatteryPanelReputation>();
		energyPReputations = new ArrayList<EnergyPanelReputation>();
		greenReputations = new ArrayList<GreenReputation>();
	
	}

	public void addQoSHistoryExperience(Service service, double experienced_utility, double declared_utility) {
		int index = (int) service.getService_id();
		QOSReputation reputation = getOrCreateQOSReputation(index);
		//if(service.getService_id()==10)
		//	System.out.println("	addQoSHistoryExperience");
		reputation.setDeclared_utility(declared_utility);
		reputation.addExperiencedUtility(experienced_utility);
	}

	public void addEnergyBPHistoryExperience(GeneralNode generalNode, double nodeBalance) {
		int index = (int) generalNode.getID();
		EnergyBatteryPanelReputation reputation = getOrCreateEnergyBPReputation(index);
		reputation.addDeclaredEnergy(nodeBalance);
	}
	public void addEnergyPHistoryExperience(GeneralNode generalNode, double nodeBalance) {
		int index = (int) generalNode.getID();
		EnergyPanelReputation reputation = getOrCreateEnergyPReputation(index);
		reputation.addDeclaredEnergy(nodeBalance);
	}
	

	public void addGreenHistoryExperience(GeneralNode generalNode, double level) {
		int index = (int) generalNode.getID();
		GreenReputation reputation = getOrCreateGreenReputation(index);
		reputation.addDeclaredEnergy(level);
	}
	
	

	/**
	 * Makes a copy of this object. Needs to be explicitly defined, since we have
	 * array members.
	 */
	@Override
	public Object clone() {
		OverloadApplication result = null;
		try {
			result = (OverloadApplication) super.clone();
		} catch (CloneNotSupportedException ex) {
			System.out.println(ex.getMessage());
			assert (false);
		}
		result.qosReputations = new ArrayList<QOSReputation>();
		result.energyBPReputations = new ArrayList<EnergyBatteryPanelReputation>();
		result.energyPReputations = new ArrayList<EnergyPanelReputation>();
		result.greenReputations = new ArrayList<GreenReputation>();
		return result;
	}

	// returns true if comp > old
	public Service chooseByStrategy(LinkedList<Service> candidates, GeneralNode node) {


		// random strategy
		if (STRATEGY.equals("random")) {
			return chooseByRandomStrategy(candidates);
		}
		if (STRATEGY.equals("weighted_random")) {
			return chooseByWeightedRandomStrategy(candidates);
		}
		// individual energy
		if (STRATEGY.equals("local_energy")) {
			return chooseByLocalEnergyStrategy(candidates, node);
		}
		// overall energy
		if (STRATEGY.equals("overall_energy")) {
			return chooseByOverallEnergyStrategy(candidates);
		}
		if (STRATEGY.equals("residual_life")) {
			return chooseByResidualLifeStrategy(candidates);
		}
		if (STRATEGY.equals("rev_residual_life")) {
			return chooseByRevResidualLifeStrategy(candidates);
		}
		if (STRATEGY.equals("latency_set")) {
			return chooseByLatencySetStrategy(candidates, node);
		}
		// fair energy
		if (STRATEGY.equals("fair_energyBP")) {
			return chooseByFairEnergyBatteryPanelStrategy(candidates, node);
		}
		if (STRATEGY.equals("fair_energyP")) {
			return chooseByFairEnergyPanelStrategy(candidates, node);
		}

		if (STRATEGY.equals("green_learning")) {
			return chooseByGreenLearningStrategy(candidates);
		}
		if (STRATEGY.equals("maxbSolarPanel")) {
			return chooseByMaxBalanceSolarPanelStrategy(candidates);
		}
		if (STRATEGY.equals("maxb")) {
			return chooseByMaxBalancePanelStrategy(candidates);
		}
		if (STRATEGY.equals("maxgPanel")) {
			return chooseByMaxGStrategy(candidates);
		}
		
		// future expected utility
		if (STRATEGY.equals("emergent")) {
			return chooseByFutureExpectedUtility(candidates, node);
		}
		
		// exception is raised if a strategy is not selected
		else {
			try {
				throw new Exception("Strategy not selected");
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.exit(0);
			return null;
		}
	}

	
	
	

	// chooses a random component
	public Service chooseByRandomStrategy(LinkedList<Service> candidates) {
		
		int index = CommonState.r.nextInt(candidates.size());
		return candidates.get( index);
	}
	

	
	/*
	 * Seleziona il servizio in modo probabilistico in base alle vite residue dei nodi
	 * */
	private Service chooseByWeightedRandomStrategy(LinkedList<Service> candidates) {
		
		if(CDState.getCycle()<7)
			return chooseByRandomStrategy(candidates);	
				
		double sum=0;
		double finite_sum=0;
		
		ArrayList<Double> array = new ArrayList<Double>();
		
		for(int i=0; i<candidates.size(); i++) {

			if(candidates.get(i).getWeight()<Double.POSITIVE_INFINITY) {
				finite_sum+=candidates.get(i).getWeight();
			}
		}	
		
		for(int i=0; i<candidates.size(); i++) {

			if(candidates.get(i).getWeight()<Double.POSITIVE_INFINITY) {
				sum+=candidates.get(i).getWeight();
				array.add(sum);	
			}else {
				sum+=finite_sum;
				array.add(sum);	
			}
		}
		
		
		double max = 0;
		double min = sum;
		
		double random_num = min + (max - min) * CommonState.r.nextDouble();

		int index=0;
		
		for(int i=0; i<array.size(); i++) {
			if(array.get(i)>random_num) {
				index = i;
				break;
			}
		}	
		
		return candidates.get(index);
	}
	
	

	// local energy strategy
	private Service chooseByLocalEnergyStrategy(LinkedList<Service> candidates, GeneralNode node) {

		if(CDState.getCycle()<7)
			return chooseByRandomStrategy(candidates);
		
		Location thisLoc = node.getLocation();

		double min=Double.MAX_VALUE;
		Service res = null;
		
		
		for(int i=0; i<candidates.size(); i++) {
			
			GeneralNode other_node = GeneralNode.getNode(candidates.get(i).getNode_id());
			double energy = 0;

			if (node.getID() == other_node.getID()) {
				
				energy += candidates.get(i).getL_comp();

				// per il modello energetico adottato ad ECSA L_comm non dipende dal nodo in
				// ricezione (i.e., node)
				energy += candidates.get(i).getL_comm();
			} else {
				
				Location other_loc = other_node.getLocation();
				double other_latency = thisLoc.latency(other_loc);

				energy += node.getConsumedIndividualCommEnergySending(1, other_latency);
			}
			
			if(energy<min) {
				min=energy;
				res=candidates.get(i);
			}
		}
		

		return res;
		
	}
	
	

	// overall energy strategy
	private Service chooseByOverallEnergyStrategy(LinkedList<Service> candidates) {

		// at round 1 the overall energy is not known
		if(CDState.getCycle()<7)
			return chooseByRandomStrategy(candidates);
				
		double min=Double.MAX_VALUE;
		Service res = null;
		
		for(int i=0; i<candidates.size(); i++) {

			double energy = candidates.get(i).getE_comp() + candidates.get(i).getE_comm();

			if(energy<min) {
				min=energy;
				res=candidates.get(i);
			}	
		}
		return res;
	}

	
	
	/*
	 * Seleziona il servizio allocato sul nodo con vita residua maggiore
	 * */
	private Service chooseByResidualLifeStrategy(LinkedList<Service> candidates) {
		
		if(CDState.getCycle()<7)
			return chooseByRandomStrategy(candidates);
		
		
		double max = 0;
		Service res = null;
		for(int i=0; i<candidates.size(); i++) {
			GeneralNode n = GeneralNode.getNode(candidates.get(i).getNode_id());
			if(n.getResidualLife()>max) {
				max = n.getResidualLife();
				res = candidates.get(i);
			}
		}
		
		
		if(res==null)
			return chooseByRandomStrategy(candidates);
		
		return res;
	}
	
	
	/*
	 * Seleziona il servizio allocato sul nodo con vita residua minore
	 * */
	private Service chooseByRevResidualLifeStrategy(LinkedList<Service> candidates) {
		
		if(CDState.getCycle()<7)
			return chooseByRandomStrategy(candidates);
		
		
		double min = Double.MAX_VALUE;
		Service res = null;
		for(int i=0; i<candidates.size(); i++) {
			GeneralNode n = GeneralNode.getNode(candidates.get(i).getNode_id());
			
			if(n.getResidualLife()<min) {
				min = n.getResidualLife();
				res = candidates.get(i);
			}
		}
	

		if(res==null)
			return chooseByRandomStrategy(candidates);
				
		return res;
	}
	
	
	
	private Service chooseByLatencySetStrategy(LinkedList<Service> candidates, GeneralNode node) {
		
		if(CDState.getCycle()<7)
			return chooseByRandomStrategy(candidates);
		
		
		double max = 0;
		double max_best = 0;
		Service res = null;
		Service res_best = null;
		for(int i=0; i<candidates.size(); i++) {
			GeneralNode n = GeneralNode.getNode(candidates.get(i).getNode_id());
			if(n.getBestNode()) {
				if (n.inPeerSet(node)) {
					if(n.getResidualLife()>max_best) {
						max_best = n.getResidualLife();
						res_best = candidates.get(i);
					}
				}
			}else {
				if(n.getResidualLife()>max) {
					max = n.getResidualLife();
					res = candidates.get(i);
				}
			}
			
			
		}
		
		if(res==null&&res_best==null)
			return chooseByRandomStrategy(candidates);
		
		if(res_best!=null)
			return res_best;
		return res;
	}

	
	
	/* greedy fair energy strategy (using Shaerf) - select the node with the "best" energy balance - problemi con G - R perchè negativo 
	 * (scenario battery - panel)
	 * */
	private Service chooseByFairEnergyBatteryPanelStrategy(LinkedList<Service> candidates, GeneralNode node) {

		if(CDState.getCycle()<7)
			return chooseByRandomStrategy(candidates);
		
		
		Double[] probl_array = new Double[candidates.size()];

		double sigma = 0;

		// tiny translation to avoid 0 as a base of the exponent	
		double translation = Math.pow(10,-5);

		for(int i=0; i<candidates.size(); i++) {

			GeneralNode nnode = GeneralNode.getNode(candidates.get(i).getNode_id());
			double cand_ee = nnode.getEeBPEnergy();
			int k = nnode.getEeBPCounter();

			// if no experiences do the average
			if (k == 0) {
				int n = 0;
				double sum = 0;

				for(int j=0; j<Network.size(); j++) {
					GeneralNode othernode = (GeneralNode) Network.get(i);
					
					if (othernode.getEeBPCounter() > 0) {
						double ee = othernode.getEeBPEnergy();
						sum += ee;
						n++;
					}
					
				}
				

				if (n == 0)
					return chooseByLocalEnergyStrategy(candidates, node);

				cand_ee = sum / n;

			}

			cand_ee-=translation;

			// energy balance -> higher is better --> negative exponent
			double cand_probl = Math.pow(cand_ee, -3);
			probl_array[i] = cand_probl;

			sigma+=cand_probl;
			
		}
	
		for(int i=0; i<probl_array.length; i++) {
			probl_array[i] = probl_array[i] / sigma;
		}
		
		
		// random pesata su probl_array
		
		
		double sum=0;
		
		ArrayList<Double> array = new ArrayList<Double>();
		
		for(int i=0; i<probl_array.length; i++) {

			sum+=probl_array[i];
			array.add(sum);
		}	
		
		double max = 0;
		double min = sum;
		
		double random_num = min + (max - min) * CommonState.r.nextDouble();

		int index=0;
		
		for(int i=0; i<array.size(); i++) {
			if(array.get(i)>random_num) {
				index = i;
				break;
			}
		}
		
		
		return candidates.get(index);
	
	}
	
	
	
	
	/* greedy fair energy strategy (using Shaerf) - select the node with the "best" energy balance - problemi con G - R perchè negativo 
	 * (scenario panel)
	 * */		
	private Service chooseByFairEnergyPanelStrategy(LinkedList<Service> candidates, GeneralNode node) {

			if(CDState.getCycle()<7)
				return chooseByRandomStrategy(candidates);
			
			
			Double[] probl_array = new Double[candidates.size()];

			double sigma = 0;

			// tiny translation to avoid 0 as a base of the exponent	
			double translation = Math.pow(10,-5);

			for(int i=0; i<candidates.size(); i++) {

				GeneralNode nnode = GeneralNode.getNode(candidates.get(i).getNode_id());
				double cand_ee = nnode.getEePEnergy();
				int k = nnode.getEePCounter();

				// if no experiences do the average
				if (k == 0) {
					int n = 0;
					double sum = 0;

					for(int j=0; j<Network.size(); j++) {
						GeneralNode othernode = (GeneralNode) Network.get(i);
						
						if (othernode.getEePCounter() > 0) {
							double ee = othernode.getEePEnergy();
							sum += ee;
							n++;
						}
						
					}
					

					if (n == 0)
						return chooseByLocalEnergyStrategy(candidates, node);

					cand_ee = sum / n;

				}

				cand_ee-=translation;

				// energy balance -> higher is better --> negative exponent
				double cand_probl = Math.pow(cand_ee, -3);
				probl_array[i] = cand_probl;

				sigma+=cand_probl;
				
			}
		
			for(int i=0; i<probl_array.length; i++) {
				probl_array[i] = probl_array[i] / sigma;
			}
			
			
			// random pesata su probl_array
			
			
			double sum=0;
			
			ArrayList<Double> array = new ArrayList<Double>();
			
			for(int i=0; i<probl_array.length; i++) {

				sum+=probl_array[i];
				array.add(sum);
			}	
			
			double max = 0;
			double min = sum;
			
			double random_num = min + (max - min) * CommonState.r.nextDouble();

			int index=0;
			
			for(int i=0; i<array.size(); i++) {
				if(array.get(i)>random_num) {
					index = i;
					break;
				}
			}
			
			
			return candidates.get(index);
		
		}

	
	
	private Service chooseByGreenLearningStrategy(LinkedList<Service> candidates) {
		
		if(CDState.getCycle()<287)
			return chooseByRandomStrategy(candidates);
		
		ArrayList<Double> probl_array = new ArrayList<Double>();
		ArrayList<Double> final_probl_array = new ArrayList<Double>();

		double sigma = 0;

		
		for(int i=0; i<candidates.size(); i++) {
			
			GeneralNode node = GeneralNode.getNode(candidates.get(i).getNode_id());

			if(node.getHistoryCounter()>286) {
				double prevision = node.getPredictedG();
	
				double cand_probl = Math.pow(prevision, 3);
				
				probl_array.add(cand_probl);
				sigma+=cand_probl;
				
			}
		}


		int counter=0;
		for(int i=0; i<probl_array.size(); i++) {
			if(probl_array.get(i)==0)
				counter++;
			final_probl_array.add(probl_array.get(i) / sigma);

		}
		
		if(counter==candidates.size())
			return chooseByRandomStrategy(candidates);

		
		double max=0;
		Service res = null;
		
		for(int i=0; i<final_probl_array.size(); i++) {
			if(final_probl_array.get(i)>max) {
				max=final_probl_array.get(i);
				res=candidates.get(i);
			}
		}		
		
		
		if(res==null)
			return chooseByRandomStrategy(candidates);

		return res;
	}
	
	
	
	/*
	 * Seleziona il servizio allocato sul nodo con maggiore bilancio energetico 
	 * (scenario batteria + pannello)
	 * */
	private Service chooseByMaxBalanceSolarPanelStrategy(LinkedList<Service> candidates) {
		
		if(CDState.getCycle()<7)
			return chooseByRandomStrategy(candidates);
		
		double max = 0;
		Service res = null;
		for(int i=0; i<candidates.size(); i++) {
			GeneralNode n = GeneralNode.getNode(candidates.get(i).getNode_id());
			double balance = n.getBattery() + n.getG()-n.getR();
			if(balance>max) {
				max = balance;
				res = candidates.get(i);
			}
		}
		
		//System.out.println(res.getService_id());
		if(res==null)
			return chooseByRandomStrategy(candidates);
		
		return res;
	}


	/*
	 * Seleziona il servizio allocato sul nodo con maggiore bilancio energetico 
	 * (scenario solo pannello)
	 * */
	private Service chooseByMaxBalancePanelStrategy(LinkedList<Service> candidates) {
		
		if(CDState.getCycle()<7)
			return chooseByRandomStrategy(candidates);
		
		double max = 0;
		Service res = null;
		for(int i=0; i<candidates.size(); i++) {
			GeneralNode n = GeneralNode.getNode(candidates.get(i).getNode_id());
			double balance = n.getG()-n.getR();
			if(balance>max) {
				max = balance;
				res = candidates.get(i);
			}
		}
		
		if(res==null)
			return chooseByRandomStrategy(candidates);
		
		return res;
	}



	private Service chooseByMaxGStrategy(LinkedList<Service> candidates) {
		
		if(CDState.getCycle()<7)
			return chooseByRandomStrategy(candidates);
		
		double max = 0;
		Service res = null;
		for(int i=0; i<candidates.size(); i++) {
			GeneralNode n = GeneralNode.getNode(candidates.get(i).getNode_id());
			double g = n.getG();
			if(g>max) {
				max = g;
				res = candidates.get(i);
			}
		}
		
		if(res==null)
			return chooseByRandomStrategy(candidates);
		
		return res;
	}



	

		
		
	
		private Service chooseByFutureExpectedUtility(LinkedList<Service> candidates, GeneralNode node) {

			// at round 1 the overall energy is not known
			if(CDState.getCycle()<7)
				return chooseByRandomStrategy(candidates);
			
						
			ArrayList<Double> probl_array = new ArrayList<Double>();
			ArrayList<Double> quality_probl_array = new ArrayList<Double>();

			
			for(int i=0; i<candidates.size(); i++) {
			
				GeneralNode n = GeneralNode.getNode(candidates.get(i).getNode_id());

				//double candidateTrust = quality_candidateReputation.getTk();
				double candidateTrust = n.getTk((int)candidates.get(i).getService_id());
				double Qk= n.getQk((int)candidates.get(i).getService_id());
				int k = (int) n.getQosCounter((int)candidates.get(i).getService_id());
						
				double candidateFEU = candidateTrust * candidates.get(i).getDeclaredUtility()
						+ ((1.0 - candidateTrust) * Qk);
				
				
				
				// if no experiences do the average
				if (k == 0) {

					int m = 0;
					double sum = 0;
					
					
					for(int j=0; j<Network.size(); j++) {
						GeneralNode othernode = (GeneralNode) Network.get(i);
						
						for(int l=0; l<5; l++) {
							int counter = othernode.getQosCounter(othernode.getID()*l);
							
							if (counter > 0) {
								double qk = othernode.getQk(othernode.getID()*l);
								sum += qk;
								m++;
							}
						}
						
							
					}


					if (m == 0)
						return chooseByLocalEnergyStrategy(candidates, node);

					candidateFEU = sum / m;
				}
				
				double cand_probl = Math.pow(candidateFEU, 40);
				probl_array.add(cand_probl);
				
			}
			
			double sigma = 0;
			
			for(int i=0; i<probl_array.size(); i++) {
				sigma += probl_array.get(i);
			}
			
			for(int i=0; i<probl_array.size(); i++) {
				quality_probl_array.add(probl_array.get(i) / sigma);
			}
			
			
			// random pesata su probl_array
			
			double sum=0;
			
			ArrayList<Double> array = new ArrayList<Double>();
			
			for(int i=0; i<quality_probl_array.size(); i++) {

				sum+=quality_probl_array.get(i);
				array.add(sum);
			}	
			
			
			double max = 0;
			double min = sum;
			
			double random_num = min + (max - min) * CommonState.r.nextDouble();

			int index=0;
			
			for(int i=0; i<array.size(); i++) {
				if(array.get(i)>random_num) {
					index = i;
					break;
				}
			}
			
			
			return candidates.get(index);
			
		}
		
	
	
	

	@Override
	public void onKill() {
		// TODO Auto-generated method stub
	}

	@Override
	public void nextCycle(Node node, int protocolID) {
	}

	private QOSReputation getOrCreateQOSReputation(int serviceId) {
		for (QOSReputation reputation : qosReputations) {
			if (reputation.getServiceID() == serviceId) {
				return reputation;
			}
		}
		QOSReputation newReputation = new QOSReputation(serviceId);
		qosReputations.add(newReputation);
		return newReputation;
	}

	private EnergyBatteryPanelReputation getOrCreateEnergyBPReputation(int nodeID) {
		for (EnergyBatteryPanelReputation reputation : energyBPReputations) {
			if (reputation.getNodeId() == nodeID) {
				return reputation;
			}
		}
		EnergyBatteryPanelReputation newReputation = new EnergyBatteryPanelReputation(nodeID);
		energyBPReputations.add(newReputation);
		return newReputation;
	}
	
	private EnergyPanelReputation getOrCreateEnergyPReputation(int nodeID) {
		for (EnergyPanelReputation reputation : energyPReputations) {
			if (reputation.getNodeId() == nodeID) {
				return reputation;
			}
		}
		EnergyPanelReputation newReputation = new EnergyPanelReputation(nodeID);
		energyPReputations.add(newReputation);
		return newReputation;
	}
	
	
	public GreenReputation getOrCreateGreenReputation(int nodeID) {
		for (GreenReputation reputation : greenReputations) {
			if (reputation.getNodeId() == nodeID) {
				return reputation;
			}
		}
		GreenReputation newReputation = new GreenReputation(nodeID);
		greenReputations.add(newReputation);
		return newReputation;
	}
	


	public ArrayList<QOSReputation> getQoSReputations() {
		return qosReputations;
	}

	public ArrayList<EnergyBatteryPanelReputation> getEnergyBPReputations() {
		return energyBPReputations;
		
	}public ArrayList<EnergyPanelReputation> getEnergyPReputations() {
		return energyPReputations;
	}
	
	public ArrayList<GreenReputation> getGreenReputations() {
		return greenReputations;
	}
	
	
	
	public void reset() {
		qosReputations = new ArrayList<QOSReputation>();
		energyBPReputations = new ArrayList<EnergyBatteryPanelReputation>();
		energyPReputations = new ArrayList<EnergyPanelReputation>();
		greenReputations = new ArrayList<GreenReputation>();

	}

}
