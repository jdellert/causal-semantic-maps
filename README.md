# causal-semantic-maps
Automated inference of diachronic and synchronic semantic maps based on causal inference.

## Installation Instructions (for Linux)
1) Make sure git and maven are installed on your machine.
2) Check out the GitHub repositories aitiotita and causal-semantic-maps into some directory:
```
somedir$ git clone https://github.com/jdellert/aitiotita.git
somedir$ git clone https://github.com/jdellert/causal-semantic-maps.git
```
3) Change to the aitiotita directory, and execute "mvn install" there:
```
somedir$ cd aitiotita
somedir/aitiotita$ mvn install
```
4) Change to the causal-semantic-maps directory, and execute "mvn install" there:
```
somedir/aitiotita$ cd ../causal-semantic-maps
somedir/causal-semantic-maps$ mvn install
```
## Running the Experiments

To rerun the experiment involving the study by François (2008) about the domain of breathing:
```
somedir/causal-semantic-maps$ java -jar target/causal-semantic-maps-1.0-jar-with-dependencies.jar -i examples/data/breathing/francois2008-isolectic-areas.tsv -vc examples/data/breathing/francois2008-concept-coordinates.tsv -vo examples/results/breathing/francois2008 -b
```
To rerun the experiment involving the study by Wilkins (1996) about bodyparts:
```
somedir/causal-semantic-maps$ java -jar target/causal-semantic-maps-1.0-jar-with-dependencies.jar -i examples/data/clics3/clics3-full.tsv -c examples/data/bodyparts/wilkins1996-clics-concepts.txt -vc examples/data/bodyparts/wilkins1996-clics-concept-coordinates.tsv -vo examples/results/bodyparts/wilkins1996 -d -b -r -lt 3
```
To rerun the experiment involving the study by Viberg (1983) about verbs of perception:
```
somedir/causal-semantic-maps$ java -jar target/causal-semantic-maps-1.0-jar-with-dependencies.jar -i examples/data/clics3/clics3-full.tsv -c examples/data/perception/viberg1983-clics-concepts.txt -vc examples/data/perception/viberg1983-clics-concept-coordinates.tsv -vo examples/results/perception/viberg1983 -d -b -r -lt 3
```
## Running Regier ea. (2013) on data in my format

To run the map inference algorithm by Regier ea. (2013) on the data by François (2008), use my adapted version of their script:
```
somedir/causal-semantic-maps$ python3 scripts/regier-adapted.py examples/data/breathing/francois2008-isolectic-areas.tsv
```
You should also be able to use this script to run the algorithm on any other dataset in my format.
