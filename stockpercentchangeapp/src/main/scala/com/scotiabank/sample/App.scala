package com.scotiabank.sample

import org.apache.commons.cli.MissingArgumentException
import org.apache.spark.SparkFiles
import org.apache.spark.sql.SparkSession

/*************************************
* StockPercentChangeApp
* Date: 7/23/2017
* Author: Ricky Leung
* Description: Given a csv file of the SP500 index over a period of time. Prints out the change value range
* where 90% of the values fall. The change value is defined as the percentage index change compared to the previous index.
* Arguments:
*   InputFilePath - The filepath to the csv containing the SP500
**************************************/
object App
{
  def main (arg: Array[String]): Unit ={

    /** **********************
      * Script
      * 1) Setup & validation checks
      * 2) Read Input Csv
      * 3) Clean/Process Data
      * 4) Calculate the Percentage change
      * 5) Determine the 90% significance
      * 6) Output Result
      * ***********************/

    /** ***********************
      * 1) Setup & validation checks
      * *************************/

    // Validation: Check if the input file argument is given
    if (arg.length < 1)
    {
      throw new MissingArgumentException("Missing input csv file argument");
    }
    // Validation: Check if file given is a csv file
    require(arg(0).endsWith(".csv"), "Not a csv file")
    val inputFile = arg(0);

    // Setup Spark Context/Session
    // Using Spark V2. If using V1 please change the pom.xml dependencies and use the V1 backward compatibility code
    val spark = SparkSession.builder()
                            .getOrCreate()

    /* /Spark v1 backward compatibility
     *   // Add the following to the import
     *   import org.apache.spark.SparkConf
     *   import org.apache.spark.SparkContext
     *
     *   val conf = new SparkConf()
     *   val sc = new SparkContext(conf)
     *   val sqlContext = new org.apache.spark.sql.SQLContext(sc)
     */

    /** ***********************
      * 2) Read Input Csv
      * *************************/
    val sourceData = spark.read
                          .option("header", true)
                          .option("inferSchema", "true")
                          .csv(SparkFiles.get(inputFile))

    /* Spark v1 backward compatibility
     *val sourceData = sqlContext.read.format("csv")
     *                           .option("header", "true")
     *                           .option("inferSchema", "true")
     *                           .load(inputFile)
     * */

    /** ***********************
      * 3)  Clean/Process data
      * *************************/

    // Filter out the holiday data as stock markets are closed
    // Holidays dates have value of "." for that corresponding date
    val pruneData = sourceData.filter(x => x(1) != ".")

    // To calculate the percentage change we need to have the previous closing day value.
    // Use the SQL lag function to generate the column values for the previous closing day
    // Also use cast values to double
    pruneData.toDF().createOrReplaceTempView("prunedDataTable")

    val dataTableWithLag = spark.sql("SELECT Date, cast(SP500 AS double) AS ClosingValue, cast(LAG(SP500) OVER (ORDER BY Date) AS double) AS PrevClosingValue FROM prunedDataTable")

    /** ***********************
      * 4)  Calculate the Percentage change
      * *************************/
    // Use the following formula
    // PercentChange = (ClosingValue - PrevClosingValue)/PrevClosingValue * 100

    // Use the HAVING clause to remove the first row in the percentage change data set since it is NULL due to the lag not having data for the 1st row
    dataTableWithLag.createOrReplaceTempView("dataTableWithLag")

    val distribution = spark.sql("SELECT (ClosingValue - PrevClosingValue)/PrevClosingValue * 100 AS PercentChange FROM dataTableWithLag HAVING ((ClosingValue - PrevClosingValue)/PrevClosingValue * 100) IS NOT NULL")

    /** ***********************
      * 5)  Calculate the 90% significance
      * *************************/
    // Assuming we want the middle 90% range, we can calculate 90% significance by calculating the 5th and 95th percentiles and using those as the lower and upper bounds.=
    val quartile_results = distribution.stat.approxQuantile("PercentChange", Array(0.05, 0.95), 0)

    /** ***********************
      * 6)  Output Result
      * *************************/
    // Since the 95th and 5th percentile could be different values and we are only allowed one +/- value according to the problem
    // Choose the max(abs(x),abs(y)) as the final value to display.
    val final_result = quartile_results.map(y => Math.abs(y))
                                       .reduceLeft(_ max _)

    println("Percentage change [-" + final_result + "%, " + final_result + "%] is within the 90% statistical significance for SP500.")
  }
}
