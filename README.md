# SWE 264P Labs
*Author: [Can Wang](mailto:canw7@uci.edu)*

## Lab 1

### Set Up Instructions
- System A
  - `cd src/main/java/Lab1/SystemA`
  - `javac *.java`
  - `java Plumber`
  > The output file is located at `src/main/java/Lab1/SystemA/OutputA.csv`
- System B
  - `cd src/main/java/Lab1/SystemB`
  - `javac *.java`
  - `java Plumber`
  > The output file is located at `src/main/java/Lab1/SystemB/WildPoints.csv` 
  > and `src/main/java/Lab1/SystemB/OutputB.csv`. 

### Notes: 
- For the implementation in System B, I changed the id of "incorrect" altitude from `2` to `6`.
  Since `6` is an unused value of each id, so when the `SinkFilter` reaches a data with an id of `6` afterward,
  the program will know the data was changed previously and should append a `*` at the end of the data
  when writing this record to the CSV file on local disk.
- In my implementation, if the altitude was changed, then the *previous altitude* for the next altitude to compare with
  will be its updated value, instead of the original one.
- The output csv files from the case I was provided with are also available at `src/main/java/Lab1/MyCaseOutput`.

*Thank you for reviewing, for any issues, feel free to contact me [here](mailto:canw7@uci.edu).  :)*


