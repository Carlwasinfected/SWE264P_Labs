/******************************************************************************************************************
* File:SinkFilter.java
* Project: Lab 1
* Copyright:
*   Copyright (c) 2020 University of California, Irvine
*   Copyright (c) 2003 Carnegie Mellon University
* Versions:
*   1.1 January 2020 - Revision for SWE 264P: Distributed Software Architecture, Winter 2020, UC Irvine.
*   1.0 November 2008 - Sample Pipe and Filter code (ajl).
*
* Description:
* This class serves as an example for using the SinkFilterTemplate for creating a sink filter. This particular
* filter reads some input from the filter's input port and does the following:
*	1) It parses the input stream and "decommutates" the measurement ID
*	2) It parses the input steam for measurments and "decommutates" measurements, storing the bits in a long word.
* This filter illustrates how to convert the byte stream data from the upstream filterinto useable data found in
* the stream: namely time (long type) and measurements (double type).
* Parameters: None
* Internal Methods: None
******************************************************************************************************************/

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SinkFilter extends FilterFramework
{
	public void run()
    {
		/************************************************************************************
		*	TimeStamp is used to compute time using java.util's Calendar class.
		* 	TimeStampFormat is used to format the time value so that it can be easily printed
		*	to the terminal.
		*************************************************************************************/
		Calendar TimeStamp = Calendar.getInstance();
		SimpleDateFormat TimeStampFormat = new SimpleDateFormat("yyyy MM dd::hh:mm:ss:SSS");

		int MeasurementLength = 8;		// This is the length of all measurements (including time) in bytes
		int IdLength = 4;				// This is the length of IDs in the byte stream
		byte databyte = 0;				// This is the data byte read from the stream
		int bytesread = 0;				// This is the number of bytes read from the stream
		long measurement;				// This is the word used to store all measurements - conversions are illustrated.
		int id;							// This is the measurement id
		int i;							// This is a loop counter
		String outputPath = "OutputB.csv";
		StringBuilder currentFrame = new StringBuilder();

		FileWriter csvWriter = null;
		try {
			if (Files.exists(Path.of(outputPath))) {
				Files.delete(Path.of(outputPath));
			}
			csvWriter = new FileWriter(outputPath, true);  // set as append mod
			String header = "Time,Velocity,Altitude,Pressure,Temperature\n";
			csvWriter.append(header);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		// First we announce to the world that we are alive...
		System.out.print( "\n" + this.getName() + "::Sink Reading ");

		while (true)
		{
			try
			{
				/***************************************************************************
				// We know that the first data coming to this filter is going to be an ID and
				// that it is IdLength long. So we first get the ID bytes.
				****************************************************************************/
				id = 0;
				for (i=0; i<IdLength; i++ )
				{
					databyte = ReadFilterInputPort();	// This is where we read the byte from the stream...
					id = id | (databyte & 0xFF);		// We append the byte on to ID...
					if (i != IdLength-1)				// If this is not the last byte, then slide the
					{									// previously appended byte to the left by one byte
						id = id << 8;					// to make room for the next byte we append to the ID
					}
					bytesread++;						// Increment the byte count
				}

				/****************************************************************************
				// Here we read measurements. All measurement data is read as a stream of bytes
				// and stored as a long value. This permits us to do bitwise manipulation that
				// is neccesary to convert the byte stream into data words. Note that bitwise
				// manipulation is not permitted on any kind of floating point types in Java.
				// If the id = 0 then this is a time value and is therefore a long value - no
				// problem. However, if the id is something other than 0, then the bits in the
				// long value is really of type double and we need to convert the value using
				// Double.longBitsToDouble(long val) to do the conversion which is illustrated below.
				*****************************************************************************/
				measurement = 0;
				for (i=0; i<MeasurementLength; i++ )
				{
					databyte = ReadFilterInputPort();
					measurement = measurement | (databyte & 0xFF);	// We append the byte on to measurement...
					if (i != MeasurementLength-1)					// If this is not the last byte, then slide the
					{												// previously appended byte to the left by one byte
						measurement = measurement << 8;				// to make room for the next byte we append to the
																	// measurement
					}
					bytesread++;									// Increment the byte count
				}

				/****************************************************************************
				// Here we look for an ID of 0 which indicates this is a time measurement.
				// Every frame begins with an ID of 0, followed by a time stamp which correlates
				// to the time that each proceeding measurement was recorded. Time is stored
				// in milliseconds since Epoch. This allows us to use Java's calendar class to
				// retrieve time and also use text format classes to format the output into
				// a form humans can read. So this provides great flexibility in terms of
				// dealing with time arithmetically or for string display purposes. This is
				// illustrated below.
				****************************************************************************/
				if ( id == 0 )
				{
					// flush if the current buffer is not empty
					if (currentFrame.length() > 0) {
						currentFrame.append('\n');
						csvWriter.write(currentFrame.toString()); // write to local disk
						currentFrame.setLength(0); // reset
					}

					// current measurement is Time.
					TimeStamp.setTimeInMillis(measurement);
					currentFrame.append(TimeStampFormat.format(TimeStamp.getTime()));
					currentFrame.append(',');
				}
				if (id == 1 || id == 2 || id == 3)
				{
					// current measurement is Velocity, Altitude (No Wild Jumps)
					currentFrame.append(Double.longBitsToDouble(measurement));
					currentFrame.append(',');
				}
				if (id == 6) {
					// current measurement is updated Altitude (Wild Jumps)
					currentFrame.append(Double.longBitsToDouble(measurement));
					currentFrame.append("*,");
				}
				if ( id == 4 )
				{
					// current measurement is temperature
					currentFrame.append(Double.longBitsToDouble(measurement));
				}
			}
			/*******************************************************************************
			*	The EndOfStreamExeception below is thrown when you reach end of the input
			*	stream. At this point, the filter ports are closed and a message is
			*	written letting the user know what is going on.
			********************************************************************************/
			catch (EndOfStreamException | IOException e)
			{
				// flush the last frame
				if (currentFrame.length() > 0) {
					currentFrame.append('\n');
					try {
						csvWriter.write(currentFrame.toString());
					} catch (IOException ex) {
						ex.printStackTrace();
					}
					currentFrame.setLength(0);
				}

				ClosePorts();
				System.out.print( "\n" + this.getName() + "::Sink Exiting; bytes read: " + bytesread );
				break;
			}
		} // while

		try {
			csvWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // run
}