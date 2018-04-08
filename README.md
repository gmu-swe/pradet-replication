# Before starting
PRADET requires the following to be available on the machine:

* java
* maven
* git
* utility commands usually pre-installed in many distributions (cat, tar, etc.)

PRADET also relies on datadep-detector to instrument and collect the data dependencies between tests, so we need to install it:

`./setup-datadep-detector.sh`

This script clone the git repo of the framework, and install it.

# Set up test subjects
Test subjects are organized according to the studies presented in the paper:
* Experimental Study (replicating DTDetector's original setup)
* Empirical Study (projects taken directly from GitHub)

## Run the Experimental Study

`cd experimental-study`

This study includes four projects:

* crystalvc (already set up)
* xmlsecurity (already set up)
* dynoptic (run `./setup-dynoptic.sh`)
* jodatime (run `./setup-jodatime.sh`)

To run the evaluation, go inside the directory of a test subject, e.g.,
`cd crystalvc`

If there a file named `test-execution-order` go ahead, otherwise see the section: 
"Generate test-execution-order file"

**NOTE**: If the test subject comes with a predefined `test-execution-order` you should use that to replicate the experimental results published in the paper. Different test execution orders might results in slightly different manifest dependencies (as discussed in the paper).

Run the following command to create the `run-order` and `reference-output` files that will be used to run PRADET.

```
../../scripts/generate_test_order.sh test-execution-order
```

### Collect data dependencies 
Assuming that you have installed datadep-detector (see above), from the test subject folder run the following commands to generate the `deps.csv` file, which the data dependencies between the tests.

```
export BIN=../../bin
export DATADEP_DETECTOR_HOME=../../datadep-detector

# Gather data about enumerations used in the test subject
../../scripts/bootstrap_enums.sh

# Create the white list package to instrument the test subject code
../../scripts/create_package_filter.sh 

# Finally start the collection
../../scripts/collect.sh 
```
A log of the collection is generated in the output folder `./pradet`

### Collect data dependencies 
After collecting the data dependencies between the tests you can refine them to discover the manifest dependencies, as follows:

```
../../scripts/refine.sh
```

At the end of the execution, PRADET prints on the console some statistics about the refinement. For example, for crystal you should get:

```
Finished after 65 iterations
Executed 222 tests in total, avg.: 3
Spent 41.0 seconds executing tests, avg.: 0.63076925 per refinement step
==================
TESTS:72
DD:65
MD:8
UD:0
EXECUTIONS:222
AVG EXECUTIONS:3
MAX EXECUTIONS:4680
TIME:41.0
AVG TIME:0.63076925
==================
Total of 42 seconds elapsed for process
```

TESTS is the side of the test suite of the application;
DD stands for Data Dependencies and correspond of the dependencies discovered during the initial collection; 
MD stands for Manifest Dependencies; 
UD stands for Untestable Dependencies;
EXECUTIONS corresponds to the overall total of test executions;
AVG EXECUTIONS is the amount of test execution per refinement iteration;
MAX EXECUTIONS is the theoretical limit of test execution which PRADET might run;
TIME the total time to complete the analysis;
AVG TIME the average time to run tests for a refinement iteration.

Details of Manifest dependencies are stored in the `refined-deps.csv` file.
Additional outputs can be found in the `./pradet` folder; those include:

* A list of schedule to replicate each manifest dependency
* refined-graph.dot a dot file which can be visualized to 
* dot files that show the progress of the refinement (you must enable the `--show-progress` option inside the `refine.sh` script

### Running DTDetector
You can compare the performance of PRADET against DTDetector by running DTDetector on the same test subject.

DTDetector comes with several configurations, for this study we consider:

* ISOLATE. Run each test in a separate VM to discover (missed) dependencies between tests. Use the script: `../../scripts/find_deps_by_isolation.sh`
* REVERSE. Run test in the opposite order than the provided one. Use the script:
`../../scripts/find_deps_by_reverse_execution.sh`
* EXHAUSTIVE with K=2. Run the tests in all the possible orders, since K=2 DTDetctor will consider only pair-wise combinations of test (as rule of thumb, if there's more than 500 tests, DTDetector might not complete in 2 days). Use the script: `../../scripts/find_deps_by_exhaustive_enumeration.sh`

To use the scripts, you must export the `DTDETECTOR_HOME` env variable as shown below:

```
export DTDETECTOR_HOME=../../bin/dtdetector
../../scripts/find_deps_by_reverse_execution.sh
```

The output of DTDetector will be produced in the `./dtd-results` folder

## Run the Empirical Study
All the test subjects in the empirical study are available from GitHub. You can download and setup all of them as follow:

```
cd empirical-study

# Clone the git repo and checkout the commit as per the paper
./setup-projects.sh 
```

To run the experiments, proceed as explained above. Note that some analysis might take 4/5 hours to complete depending on your machine and the amount of resources available.

# Generate test-execution-order file
To generate a test execution file, run the following:

```
# Execute tests using maven and 
# parse the output to the
# maven_test_execution_order file
../../scripts/extract_test_names_from_maven_output.sh
```

The `maven_test_execution_order` contains **a** possible test execution order.
If you are not satisfied with this file, you can swap lines to reorder the test executions as you wish.  Once you have done, run the following command to create the `run-order` and `reference-output` files that will be used to run PRADET.

```
../../scripts/generate_test_order.sh maven_test_execution_order
```

# Contact
PRADET is an university prototype, we tested it using 20-ish open source projects, but it is far from perfect. In case of problems you can contact alessio(dot)gambi(at)uni-passau.de.