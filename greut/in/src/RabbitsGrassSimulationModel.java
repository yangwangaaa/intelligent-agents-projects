import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;

import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.reflector.RangePropertyDescriptor;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.util.SimUtilities;

/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author Yoan Blanc <yoan.blanc@epfl.ch>
 */
public class RabbitsGrassSimulationModel extends SimModelImpl {
    private int MIN_GRID_SIZE = 10;
    private int MAX_GRID_SIZE = 100;
    private int DEFAULT_GRID_SIZE = 20;
    private int MIN_NUMBER_OF_RABBITS = 0;
    private int MAX_NUMBER_OF_RABBITS = 500;
    private int DEFAULT_NUMBER_OF_RABBITS = 150;
    private int MIN_BIRTH_THRESHOLD = 1;
    private int MAX_BIRTH_THRESHOLD = 20;
    private int DEFAULT_BIRTH_THRESHOLD = 15;
    private int MIN_GRASS_GROWTH_RATE = 0;
    private int MAX_GRASS_GROWTH_RATE = 50;
    private int DEFAULT_GRASS_GROWTH_RATE = 15;

    // simulation parameters
    private int gridSize = DEFAULT_GRID_SIZE; // 0 - 100
    private int numberOfRabbits = DEFAULT_NUMBER_OF_RABBITS; // 0 - 500
    private int birthThreshold = DEFAULT_BIRTH_THRESHOLD; // 0 - 20
    private int grassGrowRate = DEFAULT_GRASS_GROWTH_RATE; // 0 - 50
    private int grassEnergy = 5; // how much energy eating grass brings you
    private int minEnergy = MIN_BIRTH_THRESHOLD;
    private int maxEnergy = MAX_BIRTH_THRESHOLD;

    private int totalBirths = 0;
    private int totalDeaths = 0;

    // simulation objects
    private Schedule schedule;
    private DisplaySurface displaySurface;
    private RabbitsGrassSimulationSpace space;
    private ArrayList<RabbitsGrassSimulationAgent> rabbits;

    // chart object
    private OpenSequenceGraph populationGraph;

    class PopulationInTime implements DataSource, Sequence {
        public Object execute() {
            return new Double(getSValue());
        }
        public double getSValue() {
            return (double)rabbits.size();
        }
    }

    class GrassInTime implements DataSource, Sequence {
        public Object execute() {
            return new Double(getSValue());
        }
        public double getSValue() {
            return (double)space.getTotalGrass();
        }
    }

    class BirthsOverTime implements DataSource, Sequence {
        public Object execute() {
            return new Double(getSValue());
        }

        public double getSValue() {
            return totalBirths;
        }
    }

    class DeathsOverTime implements DataSource, Sequence {
        public Object execute() {
            return new Double(getSValue());
        }

        public double getSValue() {
            return totalDeaths;
        }
    }

    public void begin() {
        buildModel();
        buildSchedule();
        buildDisplay();

        displaySurface.display();
        populationGraph.display();
    }

    public void buildModel() {
        space = new RabbitsGrassSimulationSpace(gridSize, gridSize);
        space.spreadGrass(grassGrowRate);

        for (int i=0; i<numberOfRabbits; i++) {
            addNewRabbit();
        }
    }

    public void buildSchedule() {
        class RabbitsGrassSimulationStep extends BasicAction {
            public void execute() {
                // Grass grows
                space.spreadGrass(grassGrowRate);

                // Rabbits step (move + eat)
                // remove all the rabbits from the board (so they can cross
                // paths) a rabbit adds itself to the board in its step method.
                for (RabbitsGrassSimulationAgent rabbit: rabbits) {
                    space.getCurrentRabbitSpace().putObjectAt(rabbit.getX(),
                            rabbit.getY(), null);
                }
                SimUtilities.shuffle(rabbits);
                int younglings = 0;
                for (RabbitsGrassSimulationAgent rabbit: rabbits) {
                    rabbit.step(space, grassEnergy);

                    /* RabbitsGrassWeeds.nlogo
                     *
                     * if energy > birth-threshold
                     *  [ set energy energy / 2
                     *    hatch 1 [fd 1 ] ]
                     *
                     * Energy is cut by two
                     */
                    if (rabbit.getEnergy() >= birthThreshold) {
                        younglings++;
                        rabbit.setEnergy(rabbit.getEnergy() / 2);
                    }
                }

                // remove dead rabbit
                totalDeaths = 0;
                for (Iterator<RabbitsGrassSimulationAgent> iter = rabbits.iterator(); iter.hasNext(); ) {
                    RabbitsGrassSimulationAgent rabbit = iter.next();
                    if (rabbit.isDead()) {
                        iter.remove();
                        totalDeaths++;
                    }
                }

                // spawn younglings
                totalBirths = younglings;
                for (int i=0; i<younglings; i++) {
                    addNewRabbit();
                }

                // update grid
                displaySurface.updateDisplay();
            }
        }

        schedule.scheduleActionBeginning(0, new RabbitsGrassSimulationStep());

        class UpdatePopulationGraph extends BasicAction {
            public void execute() {
                populationGraph.step();
            }
        }

        schedule.scheduleActionAtInterval(5, new UpdatePopulationGraph(), Schedule.LAST);
    }

    public void buildDisplay() {
        ColorMap map = new ColorMap();
        for (int i=1; i<16; i++) {
            map.mapColor(i, new Color(0, i * 8 + 127, 0));
        }
        map.mapColor(0, Color.black);
        Value2DDisplay displayGrass = new Value2DDisplay(space.getCurrentGrassSpace(), map);
        Object2DDisplay displayRabbit = new Object2DDisplay(space.getCurrentRabbitSpace());
        displayRabbit.setObjectList(rabbits);

        displaySurface.addDisplayableProbeable(displayGrass, "Grass");
        displaySurface.addDisplayableProbeable(displayRabbit, "Rabbits");

        populationGraph.addSequence("Population", new PopulationInTime());
        populationGraph.addSequence("Grass amount", new GrassInTime());
        // Not sure if useful
        //populationGraph.addSequence("Births", new BirthsOverTime());
        //populationGraph.addSequence("Deaths", new DeathsOverTime());
    }

    private void addNewRabbit() {
        RabbitsGrassSimulationAgent rabbit = new RabbitsGrassSimulationAgent(
                minEnergy, maxEnergy, birthThreshold);
        rabbits.add(rabbit);
        space.addRabbit(rabbit);
    }

    public String[] getInitParam() {
        return new String[]{ "GridSize", "Population", "BirthThreshold",
                "GrassGrowRate" };
    }

    public int getGridSize() {
        return gridSize;
    }

    public void setGridSize(int size) {
        this.gridSize = size;
    }

    public int getPopulation() {
        return numberOfRabbits;
    }

    public void setPopulation(int population) {
        numberOfRabbits = population;
    }

    public int getBirthThreshold() {
        return birthThreshold;
    }

    public void setBirthThreshold(int threshold) {
        birthThreshold = threshold;
    }

    public int getGrassGrowRate() {
        return grassGrowRate;
    }

    public void setGrassGrowRate(int rate) {
        grassGrowRate = rate;
    }

    public String getName() {
        return "Rabbit";
    }

    public Schedule getSchedule() {
        return schedule;
    }

    @SuppressWarnings("unchecked")
    public void setup() {
        RangePropertyDescriptor pdGridSize = new RangePropertyDescriptor(
                "GridSize", MIN_GRID_SIZE, MAX_GRID_SIZE, DEFAULT_GRID_SIZE);
        RangePropertyDescriptor pdPopulation = new RangePropertyDescriptor(
                "Population", MIN_NUMBER_OF_RABBITS, MAX_NUMBER_OF_RABBITS,
                DEFAULT_NUMBER_OF_RABBITS);
        RangePropertyDescriptor pdBirthThreshold = new RangePropertyDescriptor(
                "BirthThreshold", MIN_BIRTH_THRESHOLD, MAX_BIRTH_THRESHOLD,
                DEFAULT_BIRTH_THRESHOLD);
        RangePropertyDescriptor pdGrassGrowthRate = new RangePropertyDescriptor(
                "GrassGrowRate", MIN_GRASS_GROWTH_RATE, MAX_GRASS_GROWTH_RATE,
                DEFAULT_GRASS_GROWTH_RATE);
        descriptors.put("GridSize", pdGridSize);
        descriptors.put("Population", pdPopulation);
        descriptors.put("BirthThreshold", pdBirthThreshold);
        descriptors.put("GrassGrowRate", pdGrassGrowthRate);

        rabbits = new ArrayList<RabbitsGrassSimulationAgent>();
        schedule = new Schedule(1);

        if(displaySurface != null) {
            displaySurface.dispose();
        }
        if(populationGraph != null) {
            populationGraph.dispose();
        }

        displaySurface = new DisplaySurface(this, "Window");
        populationGraph = new OpenSequenceGraph("Population report graph", this);

        registerDisplaySurface("Window", displaySurface);
        registerMediaProducer("Plot", populationGraph);
    }
}
