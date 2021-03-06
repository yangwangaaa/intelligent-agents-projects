/**
 * 
 */
package epfl.lia.logist.testing.messaging;

import epfl.lia.logist.agent.behavior.Behavior;
import epfl.lia.logist.agent.behavior.response.IBehaviorResponse;
import epfl.lia.logist.agent.behavior.response.ReadyBehaviorResponse;
import epfl.lia.logist.exception.BehaviorExecutionError;
import epfl.lia.logist.exception.BehaviorNotImplementedError;
import epfl.lia.logist.messaging.signal.Signal;
import epfl.lia.logist.logging.LogSeverityEnum;


/**
 * @author malves
 *
 */
public class Agent2Behavior extends Behavior {

	/**
	 * 
	 */
	public Agent2Behavior() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see epfl.lia.logist.agent.behavior.Behavior#execute(epfl.lia.logist.messaging.signal.Signal)
	 */
	@Override
	public IBehaviorResponse execute(Signal s) throws 
		BehaviorExecutionError, BehaviorNotImplementedError {
		
				// execution depends on the type
				switch( s.getType() ) {
					case SMT_INIT:
						log( LogSeverityEnum.LSV_INFO, "Agent2 initialized himself...'" );
						return new ReadyBehaviorResponse();
						
					case SMT_TEXT:
						log( LogSeverityEnum.LSV_INFO, "Agent2 received message '" + s.getMessage() + "'" );
						return new ReadyBehaviorResponse();
						
					case SMT_KILL:
						log( LogSeverityEnum.LSV_INFO, "Agent2 is killing himself..." );
						return new ReadyBehaviorResponse();
						
					default:
						throw new BehaviorNotImplementedError( s.getType() );
				}
	}

}
