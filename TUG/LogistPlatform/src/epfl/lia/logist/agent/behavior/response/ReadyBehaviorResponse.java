/**
 * 
 */
package epfl.lia.logist.agent.behavior.response;

/**
 * @author malves
 *
 */
public class ReadyBehaviorResponse implements IBehaviorResponse {

	/* (non-Javadoc)
	 * @see epfl.lia.logist.agent.behavior.response.BehaviorResponse#getType()
	 */
	public BehaviorResponseTypeEnum getType() {
		return BehaviorResponseTypeEnum.BRT_READY;
	}
}
