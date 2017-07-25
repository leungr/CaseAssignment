SP500 Case Assignment
===================
By:Ricky Leung

Problem Statement
-------------

The Standard & Poor's 500, often abbreviated as the S&P 500, is an American stock market index representing the performance of the overall stock market in the US. 

In this case assignment, the change of S&P 500 is defined as the percentage of index change compared to the previous closing index. For example, yesterday's S&P 500 is at 1000 when stock closed; today's S&P 500 is at 
1002 when stock closed; in this case, the change of S&P 500 today is 2%. 

We will be writing a Spark application in Scala to find out a range [-x%, x%], so that the probability of "the change of S&P falls in this range" is around 90% statistically. For example, if of the S&P 
change falls in the range of [-5%, 5%], your application should output 5% as the result. 

#### Dataset source

S&P 500 data is available in csv format at:  
https://fred.stlouisfed.org/series/SP500/downIoaddata 

----------

# Solution Design

----------
## General Design

   1) Clean/Process Data
   2) Calculate the Percentage change
   3) Calculate the 90% significance
  
### Calculations 
There are two main calculations to this problem:

 > 1) Calculating the change of the S&P 500
 > 2) Finding the range so the probability of S&P 500 is around 90% statistically
 
 **Calculating the change of the S&P 500**

Here we use the formula for a given day ***d***:

 ![PercentChangeFormula](https://lh3.googleusercontent.com/-H6f0fhUxwuk/WXaXdvFWlWI/AAAAAAAAAAg/EiLGlNx4Ya8FIuB6-obouTQxU8scGJ4YACLcBGAs/s0/PercentChangeFormula.png "PercentChangeFormula.png")

Since the formula depends on closing price for current day ***d*** as well as the previous day ***d-1*** we will require in the data cleaning/preparation step that both prices are available prior to the calculation.

**Finding the range so the probability of the S&P 500 is around 90% statistically**

Once the percent change is calculated across all days, we will get a distribution. We can obtain the middle 90% range  by calculating the  5% and 95% quantiles  as the lower and upper bounds respectively.

![ExampleDistributionFor90PercentRange](https://lh3.googleusercontent.com/-GXo9iIPAM10/WXagIDcO-1I/AAAAAAAAABA/StVppi5kXCgS88u18f1hwDgOSUTsuOXOgCLcBGAs/s0/SampleDistribution.png "ExampleDistribution.png")

Since the problem statement allows only one output vale of x for the range [-x%, x%] and that the 5% and 95% quantiles could potentially be different numbers we will take the max of the two values to output as x.  See [Extension](#extension) section for improving the design.

![MaxFormula](https://lh3.googleusercontent.com/-n25g8yN-SMM/WXaXlxakCvI/AAAAAAAAAAo/bqQrUTROIIoTXWU2EU0xDXkLHECDvCK8ACLcBGAs/s0/MaxFormula.png "MaxFormula.png")

### Data Cleaning/Preparation

>1) From the exploratory analysis of looking at the initial dataset, some
   of the closing prices were not present due to holidays

Date     | SP500
-------- | ---
2007-06-29 | 1503.35
2007-07-02    | 1519.43
2007-07-03    | 1524.87
2007-07-04 |.
2007-07-05 |1525.4

Filter out the missing data from the data set prior to calculations. Formula for percent change still applicable as previous closing price might not be consecutive days. 

Date     | SP500
-------- | ---
2007-06-29 | 1503.35
2007-07-02    | 1519.43
2007-07-03    | 1524.87
2007-07-05 |1525.4

 >2)  From the calculation of the percentage change step we require the previous closing price to available
 
Use the SQL lag function (or equvalient) to obtain the previous closing price

Date     | SP500 |PrevSP500
-------- | ---| -------
2007-06-29 | 1503.35 | NULL
2007-07-02    | 1519.43 | 1503.35 
2007-07-03    | 1524.87 | 1519.43 
2007-07-05 |1525.4 | 1524.87

### Code Reference

Main files are the App.scala under the [src folder](https://github.com/leungr/CaseAssignment/blob/master/stockpercentchangeapp/src/main/scala/com/scotiabank/sample/App.scala)

A copy of the dataset is saved under the [data folder](https://github.com/leungr/CaseAssignment/blob/master/stockpercentchangeapp/data/SP500.csv)

----------

# Building/Running

----------
An copy of the executable jar is already available to use under the [out/artifacts folder](https://github.com/leungr/CaseAssignment/tree/master/stockpercentchangeapp/out/artifacts/stockpercentchangeapp_jar)  

### Building the jar

This program was built using [IntelliJ IDEA IDE](https://www.jetbrains.com/idea/download/) with Maven and Scala. 

>1) Clone repository
>2) Build jar 
  >- **IntelliJ** ```Build -> Artifacts -> jar``` to build the jar  (it should be created in the out folder)
  >- **Command line** ```mvn package```

### Submitting Spark job

**Note**: This jar expects filepath to the csv as an input argument. If the file is not already uploaded to HDFS beforehand you can use the **--files** command argument to submit the csv with the spark job.

**Spark submit example**:
 
 ```bin\spark-submit --class com.scotiabank.sample.App --master local[4] --files ..\<Path to data>\SP500.csv ..\<Path to jar>\stockpercentchangeapp.jar SP500.csv```

If running from the clone repository careful of the file path to the .jar file as the directory name stockpercentchangeapp_jar is very similar in name to stockpercentchangeapp.jar file itself.
 
\stockpercentchangeapp\out\artifacts\stockpercentchangeapp_jar\stockpercentchangeapp.jar 

**Spark job output**:
>```Percentage change [-2.006451387514122%, 2.006451387514122%] is within the 90% statistical significance for SP500.```

-------------------

# Extension

-------------------
This case assignment can be extended to improve on the existing solution

1) **Allow different values in the percent change range [x,y]%**
   
 With index data the distribution may not always have a median around 0 and the 5% and 95% quantiles can be different numbers. Allow design change to reflect the true %5 and 95% values instead of forcing them to be one number.
 
 >Dev tasks:
 >  1) Remove logic to take max of the two quantile values
 >  2) Update the output line to reflect new values
 >* Dev work estimate: Very low
 
 2) **Adjustable statistical significance**

Pass a value to determine statistical significance instead of it being hardcoded to 90% range

 >Dev tasks:
 >  1) Read in argument and do validation checks if needed
 >  2) Add a calculation prior to the qunatile calculation to determine the quantile splits
 >  3) Update the output line to reflect the change
 >* Dev work estimate: Low
 
  3) **Read in other stock or stock indexes**

Depending on design: 

(Simple design) - Other stocks/stock indexes have the same data format as the SP500 

(Aggregated design) - Similar to the SP500 data but with an additional column that has the StockName (or StockName ID).

| Date | Stock Index Name | Price
|----|----|---
|2007-06-29 | SP500 | 1503.35 
|2007-06-29 | NASDAQ | 5000.25 
|2007-07-02 | SP500  | 1519.43 
|2007-07-02 |NASDAQ  | 5004.24

 >Dev tasks (Simple Design):
 >  1) Extract header information on stock name from CSV
 >  2) Update the SQL statements to use the header variable names instead of SP500
 >  3) Update the output line to use header variable name instead of SP500
 >* Dev work estimate: Low

 >Dev tasks (Aggregated Design):
 >  1) Update the data processing/SQL statements to include new column in the SELECT and PARTITION BY
 >  2) Update the quantile calculation to group by stock name prior to calculation
 >  3) Update the output to output multiple lines
 >  4) Depending on if we need want both solutions in one app fork logic depending if this is a two-column or three-column data.
 >* Dev work estimate: High
 
 4) **Modularize the Input file reader**

If multiple calculations are going to be done on the same dataset; modularize the input file reading code so it can be reused across apps/calculations. Also can add support for other file types (txt, tsv, etc.). Similar extension can be applied to the data cleaning/data preparation steps if they are going to be common code across different calculations/apps.

> Dev tasks:
>  1) Refactor input reading code into class methods/functions to be put in a library for usage
>  2) Add support different file types
>  3) Create unit tests against class methods/functions
>* Dev work estimate: Medium-High
