import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/******************************************************************************************************************
* File:MiddleFilter.java
* Project: Lab 1
* Copyright:
*   Copyright (c) 2020 University of California, Irvine
*   Copyright (c) 2003 Carnegie Mellon University
* Versions:
*   1.1 January 2020 - Revision for SWE 264P: Distributed Software Architecture, Winter 2020, UC Irvine.
*   1.0 November 2008 - Sample Pipe and Filter code (ajl).
*
* Description:
* This class serves as an example for how to use the FilterRemplate to create a standard filter. This particular
* example is a simple "pass-through" filter that reads data from the filter's input port and writes data out the
* filter's output port.
* Parameters: None
* Internal Methods: None
******************************************************************************************************************/

public class MiddleFilter extends FilterFramework
{
	private static double preAltitude = Double.MAX_VALUE;
	private static double ppreAltiture = Double.MAX_VALUE;

	public void run()
    {
		int bytesread = 0;					// Number of bytes read from the input file.
		int byteswritten = 0;				// Number of bytes written to the stream.
		byte databyte = 0;					// The byte of data read from the file
		int i;
		int id;
		int ID_LENGTH = 4;
		long val; // aka. measurement
		int VAL_LENGTH = 8;
		double currentAltitude;
		FileWriter csvWriter = null;
		try {
			csvWriter = new FileWriter("WildPoints.csv", true);  // set as append mod
			String header = "Time,Velocity,Altitude,Pressure,Temperature\n";
			csvWriter.append(header);
		} catch (IOException e) {
			e.printStackTrace();
		}


		// Next we write a message to the terminal to let the world know we are alive...
		System.out.print( "\n" + this.getName() + "::Middle Reading ");

		while (true)
		{
			// Here we read a byte and write a byte
			try
			{
				// get current id
				id = 0;
				for (i=0; i<ID_LENGTH; i++ )
				{
					databyte = ReadFilterInputPort();	// This is where we read the byte from the stream...
					id = id | (databyte & 0xFF);		// We append the byte on to ID...
					if (i != ID_LENGTH-1)				// If this is not the last byte, then slide the
					{									// previously appended byte to the left by one byte
						id = id << 8;					// to make room for the next byte we append to the ID
					}
					bytesread++;						// Increment the byte count
				}

				// get current val (long type, 8 bytes, 64 bits
				val = 0;
				for (i=0; i<VAL_LENGTH; i++ )
				{
					databyte = ReadFilterInputPort();
					val = val | (databyte & 0xFF);	// We append the byte on to measurement...
					if (i != VAL_LENGTH-1)					// If this is not the last byte, then slide the
					{												// previously appended byte to the left by one byte
						val = val << 8;				// to make room for the next byte we append to the
						// measurement
					}
					bytesread++;									// Increment the byte count
				}

				// check the alt of adjacent frame, see if it needs to revise
				if (id == 2) {
					currentAltitude = updateAltWhenWildJumps(Double.longBitsToDouble(val));

					// update
					ppreAltiture = preAltitude;
					preAltitude = currentAltitude;
					if (currentAltitude != Double.longBitsToDouble(val)) {
						// replacement occurred
						id = 6;

						// write wild point record to local disk
						StringBuilder sb = new StringBuilder();
						sb.append(Double.longBitsToDouble(val));
						sb.append('\n');
						csvWriter.write(sb.toString());
						sb.setLength(0);
					}
				}


				/* Write date byte by byte to the next filter */
				byte[] idBufferArray = ByteBuffer.allocate(ID_LENGTH).putInt(id).array();
				byte[] valBufferArray = ByteBuffer.allocate(VAL_LENGTH).putLong(val).array();

				for(i = 0; i < ID_LENGTH; ++i) {
					WriteFilterOutputPort(idBufferArray[i]);
					byteswritten++;
				}
				for(i = 0; i < VAL_LENGTH; ++i) {
					WriteFilterOutputPort(valBufferArray[i]);
					byteswritten++;
				}

			}
			catch (EndOfStreamException | IOException e)
			{
				ClosePorts();
				System.out.print( "\n" + this.getName() + "::Middle Exiting; bytes read: " + bytesread + " bytes written: " + byteswritten );
				break;
			}
		}
		try {
			assert csvWriter != null;
			csvWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 *
	 * @param now:
	 *
	 * @return:
	 */
   private double updateAltWhenWildJumps(double now) {
	   if (preAltitude == Double.MAX_VALUE) {
			// met first frame, no chance to update, so do nothing here

	   } else {
		   if (ppreAltiture == Double.MAX_VALUE) {
			   // met second frame
			   if (Math.abs(now - preAltitude) > 100) {
				   // simply replace the current with the previous
				   now = preAltitude;
			   }

		   } else {
			   // met third frame or afterwards
			   if (Math.abs(now - preAltitude) > 100) {
				   now = (preAltitude + ppreAltiture) / 2.0;
			   }
		   }
	   }

	   return now;
   }
}