package epfl.lia.logist.logging;


/**
 * A log descriptor structure contains the fields necessary to describe 
 * a log file. 
 */
public class LogDescriptor {
	
	/**
	 * The name of this log entry
	 * 
	 * @note If the name of the log is not available, then the log
	 *       file will not be created, without any further warning. The
	 *       event will however be stored in the primary log.
	 */
	public String ID;

	/**
	 * The file storing the log data. This file is not used for stream
	 * log files.
	 */
	public String File;

	/**
	 * The format used to format entries in a log file. Possible entries are:
	 * <ul>
	 * <li><em>xml</em> for xml formatted output files
	 * <li><em>rtf</em> for richtext colored output files
	 * <li><em>raw</em> for raw text output files
	 * <li><em>stream</em> for output on stream
	 * <li><em>custom</em> for custom formatted output files
	 * </ul>
	 * @note If the format is 'stream', then the user must pass
	 *       in the FormatClass the name of a class that inherits
	 *       from outputstream.
	 *       If the format is 'custom', then the user specifies the
	 *       the formatting class in the class field. Note however
	 *       that in this case, the formatting class must inherit
	 *       from the LogOutputFormat interface.
	 */
	public String Format; // �custom�, �xml�, �rtf�, �raw�

	/**
	 * The name of the formatting class. This field is only used
	 * for custom output formats or for streams.
	 */
	public String FormatClass = null;

	/**
	 * The lowest possible severity level. The debug level can be one
	 * of the values:
	 * <ul>
	 * 	 <li>DEBUG</li>
	 *   <li>INFO</li>
	 *   <li>WARNING</li>
	 *   <li>ERROR</li>
	 *   <li>FATAL</li>
	 * </ul>
	 * @note If the level is not recognised, then it is defaulted to
	 *       debug.
	 */
	public String DebugLevel = "DEBUG";

	/**
	 * The maximum number of entries for this log. If the number
	 * of entries overflows the maximum number of entries, then no
	 * other entry is appended.
	 */
	public int MaxEntries = 0; // infinity
	
	/**
	 * The number of entries cached before writing to disk. The
	 * cache is used to hide disk access latency
	 */
	public int CacheSize = 1; // write cache after each log
	
	/**
	 * Indicate if the log entries also should be displayed
	 * in the main stdout console.
	 */
	public boolean ToStdout = false;
	
}