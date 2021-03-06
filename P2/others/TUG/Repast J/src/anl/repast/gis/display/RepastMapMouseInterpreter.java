package anl.repast.gis.display;

//  **********************************************************************
//  
//  <copyright>
//  
//   BBN Technologies, a Verizon Company
//   10 Moulton Street
//   Cambridge, MA 02138
//   (617) 873-8000
//  
//   Copyright (C) BBNT Solutions LLC. All rights reserved.
//  
//  </copyright>
//  **********************************************************************
//  
//  $Source: /cvsroot/repast/repastj/src/anl/repast/gis/display/RepastMapMouseInterpreter.java,v $
//  $RCSfile: RepastMapMouseInterpreter.java,v $
//  $Revision: 1.5 $
//  $Date: 2004/11/03 19:51:00 $
//  $Author: jerryvos $
//  
//  **********************************************************************


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.bbn.openmap.event.MapMouseEvent;
import com.bbn.openmap.layer.OMGraphicHandlerLayer;
import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.omGraphics.OMGraphicList;
import com.bbn.openmap.omGraphics.event.GestureResponsePolicy;
import com.bbn.openmap.omGraphics.event.MapMouseInterpreter;
import com.bbn.openmap.util.Debug;

 /**
  * The StandardMapMouseInterpreter is a basic implementation of the
  * MapMouseInterpreter, working with an OMGraphicHandlerLayer to
  * handle MouseEvents on it.  This class allows the
  * OMGraphicHandlerLayer, which implements the GestureResponsePolicy,
  * to not have to deal with MouseEvents and the OMGraphicList, but to
  * just react to the meanings of the user's gestures.<p>
  *
  * The StandardMapMouseInterpreter uses highlighing to indicate that
  * mouse movement is occuring over an OMGraphic, and gives the layer
  * three ways to react to that movement.  After finding out if the
  * OMGraphic is highlightable, the SMMI will tell the layer to
  * highlight the OMGraphic (which usually means to call select() on
  * it), provide a tool tip string for the OMGraphic, and provide a
  * string to use on the InformationDelegator info line.  The layer can
  * reply or ignore any and all of these notifications, depending on
  * how it's supposed to act.<p>
  *
  * For left mouse clicks, the SMMI uses selection as a notification
  * that the user is choosing an OMGraphic, and that the OMGraphic
  * should be prepared to be moved, modified or deleted.  For a single
  * OMGraphic, this is usually handled by handing the OMGraphic off to
  * the OMDrawingTool.  However the GestureResponsPolicy handles the
  * situation where the selection is of multiple OMGraphics, and the
  * layer should prepare to handle those situations as movment or
  * deletion notifications.  This usually means to change the
  * OMGraphic's display to indicate that the OMGraphics have been
  * selected.  Selection notifications can come in series, and the
  * GestureResponsePolicy is expected to keep track of which OMGraphics
  * it has been told are selected.  Deselection notifications may come
  * as well, or other action notifications such as cut or copy may
  * arrive.  For cut and copy notifications, the OMGraphics should be
  * removed from any selection list.  For pastings, the OMGraphics
  * should be added to the selection list.<p>
  *
  * For right mouse clicks, the layer will be provided with a
  * JPopupMenu to use to populate with options for actions over a
  * OMGraphic or over the map.<p>
  *
  * The StandardMapMouseInterpreter uses a timer to pace how mouse
  * movement actions are responded to.  Highlight reactions only occur
  * after the mouse has paused over the map for the timer interval, so
  * the application doesn't try to respond to constantly changing mouse
  * locations.  You can disable this delay by setting the timer
  * interval to zero.
  * 
  * @author Robert Najlis (based on OpenMap code)
  */
 public class RepastMapMouseInterpreter  implements MapMouseInterpreter { //extends StandardMapMouseInterpreter 
    

     protected boolean DEBUG = false;
     protected OMGraphicHandlerLayer layer = null;
     protected String[] mouseModeServiceList = null;
     protected String lastToolTip = null;
     protected GestureResponsePolicy grp = null;
     protected RepastGeometryOfInterest clickInterest = null;
     protected RepastGeometryOfInterest movementInterest = null;
     protected boolean consumeEvents = false;

     /**
      * The OMGraphicLayer should be set at some point before use.
      */
     public RepastMapMouseInterpreter() {
         DEBUG = Debug.debugging("grp");
     }

     /**
      * The standard constructor.
      */
     public RepastMapMouseInterpreter(OMGraphicHandlerLayer l) {
         this();
         setLayer(l);
     }

     
     /**
      * A flag to tell the interpreter to be selfish about consuming
      * MouseEvents it receives.  If set to true, it will consume
      * events so that other MapMouseListeners will not receive the
      * events.  If false, lower layers will also receive events, which
      * will let them react too.  Intended to let other layers provide
      * information about what the mouse is over when editing is
      * occuring.
      */
     public void setConsumeEvents(boolean consume) {
         consumeEvents = consume;
     }

     public boolean getConsumeEvents() {
         return consumeEvents;
     }

     public void setLayer(OMGraphicHandlerLayer l) {
         layer = l;
     }

     public OMGraphicHandlerLayer getLayer() {
         return layer;
     }

     /**
      * Set the ID's of the mouse modes that this interpreter should be
      * listening to.  If set to null, this SMMI won't receive
      * MouseEvents.
      */
     public void setMouseModeServiceList(String[] list) {
         mouseModeServiceList = list;
     }

     /**
      * A method to set how a left mouse button is interpreted.  We
      * count control-clicks as not a left mouse click.
      */
     public boolean isLeftMouseButton(MouseEvent me) {
         return SwingUtilities.isLeftMouseButton(me) && !me.isControlDown();
     }

     /**
      * Return a list of the modes that are interesting to the
      * MapMouseListener.  You MUST override this with the modes you're
      * interested in, or set the mouse mode service list, or you won't
      * receive mouse events.
      */
     public String[] getMouseModeServiceList() {
         return mouseModeServiceList;
     }

     /**
      * Set the RepastGeometryOfInterest as one that could possibly be in the
      * process of being clicked upon.
      */
     protected void setClickInterest(RepastGeometryOfInterest goi) {
         clickInterest = goi;
     }

     /**
      * Get the RepastGeometryOfInterest as one that could possibly be in the
      * process of being clicked upon.
      */
     protected RepastGeometryOfInterest getClickInterest() {
         return clickInterest;
     }

     /**
      * Set the RepastGeometryOfInterest for something that the mouse is
      * over.  Prevents excessive modifications of the GUI if this
      * remains constant.
      */
     protected void setMovementInterest(RepastGeometryOfInterest goi) {
         movementInterest = goi;
     }

     /**
      * Get the RepastGeometryOfInterest for something that the mouse is
      * over.  Prevents excessive modifications of the GUI if this
      * remains constant.
      */
     protected RepastGeometryOfInterest getMovementInterest() {
         return movementInterest;
     }

     /**
      * Return the OMGraphic object that is under a mouse event
      * occurance on the map, null if nothing applies.
      */
     public OMGraphic getGeometryUnder(MouseEvent me) {
         OMGraphic omg = null;
         OMGraphicList list = null;
         if (layer != null) {
             list = layer.getList();
             if (list != null) {
                 omg = list.findClosest(me.getX(), me.getY(), 4);
             } else {
                 if (DEBUG) {
                     Debug.output("SMMI: no layer to evaluate mouse event");
                 }
             }
         } else {
             if (DEBUG) {
                 Debug.output("SMMI: no layer to evaluate mouse event");
             }
         }

         return omg;
     }

     // Mouse Listener events
     ////////////////////////

     /**
      * Invoked when a mouse button has been pressed on a component.
      * @param e MouseEvent
      * @return false if nothing was pressed over, or the consumeEvents
      * setting if something was.
      */
     public boolean mousePressed(MouseEvent e) { 
         if (DEBUG) {
             Debug.output("SMMI: mousePressed()");
         }
         setCurrentMouseEvent(e);
         boolean ret = false;

         RepastGeometryOfInterest goi = getClickInterest();
         OMGraphic omg = getGeometryUnder(e);

         if (goi != null && !goi.appliesTo(omg, e)) {
             // If the click doesn't match the geometry or button
             // of the geometry of interest, need to tell the goi
             // that is was clicked off, and set goi to null.
             if (goi.isLeftButton()) {
                 leftClickOff(goi.getGeometry(), e);
             } else {
                 rightClickOff(goi.getGeometry(), e);
             }
             setClickInterest(null);
         }

         if (omg != null) {
             if (isLeftMouseButton(e)) {
                 select(omg);
             }
             setClickInterest(new RepastGeometryOfInterest(omg, e, this));
             ret = true;
         }

         return ret && consumeEvents;
     }

     /**
      * Invoked when a mouse button has been released on a component.
      * @param e MouseEvent
      * @return false
      */
     public boolean mouseReleased(MouseEvent e) {
         setCurrentMouseEvent(e);
         return false;
     }

     /**
      * Invoked when the mouse has been clicked.  Notifies the left,
      * right click methods for the applicable OMGraphic or the map.
      * @param e MouseEvent
      * @return the consumeEvents setting.
      */
     public boolean mouseClicked(MouseEvent e) {
         setCurrentMouseEvent(e);
         RepastGeometryOfInterest goi = getClickInterest();

         // If there is a click interest
         if (goi != null) {
             // Tell the policy it an OMGraphic was clicked.
             if (isLeftMouseButton(e)) {
                 leftClick(goi.getGeometry(), e);
             } else {
                 rightClick(goi.getGeometry(), e);
             }
         } else {
             if (isLeftMouseButton(e)) {
                 leftClick(e);
             } else {
                 rightClick(e);
             }
         }

         return consumeEvents;
     }

     /**
      * Invoked when the mouse enters a component.
      * @param e MouseEvent
      */
     public void mouseEntered(MouseEvent e) {
         setCurrentMouseEvent(e);
     }

     /**
      * Invoked when the mouse exits a component.
      * @param e MouseEvent
      */
     public void mouseExited(MouseEvent e) {
         setCurrentMouseEvent(e);
     }

     // Mouse Motion Listener events
     ///////////////////////////////

     /**
      * Invoked when a mouse button has been pressed and is
      * moving. Resets the click geometry of interest to null.
      * 
      * @param e MouseEvent
      * @return the result from mouseMoved (also called from this
      * method) combined with the consumeEvents setting.
      */
     public boolean mouseDragged(MouseEvent e) {
         setCurrentMouseEvent(e);
         RepastGeometryOfInterest goi = getClickInterest();
         if (goi != null) {
             setClickInterest(null);
         }

         return mouseMoved(e) && consumeEvents;
     }

     /**
      * Invoked when the mouse has been moved.  Sets the movement
      * geometry of interest and updates the movement timer.
      *
      * @param e MouseEvent
      * @return the result of updateMouseMoved() if the timer isn't
      * being used, or false.
      */
     public boolean mouseMoved(MouseEvent e) {
         setCurrentMouseEvent(e);
         //boolean ret = false;

         if ((noTimerOverOMGraphic && getMovementInterest() != null) || mouseTimerInterval <= 0) {
             return updateMouseMoved(e);
         } else {
             if (mouseTimer == null) {
                 mouseTimer = new Timer(mouseTimerInterval, mouseTimerListener);
                 mouseTimer.setRepeats(false);
             }

             mouseTimerListener.setEvent(e);
             mouseTimer.restart();
             return false;
         }
     }

     protected boolean noTimerOverOMGraphic = true;

     /**
      * Set whether to ignore the timer when movement is occuring over
      * an OMGraphic.  Sometimes unhighlight can be inappropriately
      * delayed when timer is enabled.
      */
     public void setNoTimerOverOMGraphic(boolean val) {
         noTimerOverOMGraphic = val;
     }

     /**
      * Get whether the timer should be ignored when movement is
      * occuring over an OMGraphic.
      */
     public boolean getNoTimerOverOMGraphic() {
         return noTimerOverOMGraphic;
     }

     /**
      * The wait interval before a mouse over event gets triggered.
      */
     protected int mouseTimerInterval = 150;

     /**
      * Set the time interval that the mouse timer waits before calling
      * upateMouseMoved.  A negative number or zero will disable the
      * timer.
      */
     public void setMouseTimerInterval(int interval) {
         mouseTimerInterval = interval;
     }

     public int getMouseTimerInterval() {
         return mouseTimerInterval;
     }

     /**
      * The timer used to track the wait interval.
      */
     protected Timer mouseTimer = null;

     /**
      * The timer listener that calls updateMouseMoved.
      */
     protected MouseTimerListener mouseTimerListener = new MouseTimerListener();

     /**
      * The definition of the listener that calls updateMouseMoved when
      * the timer goes off.
      */
     protected class MouseTimerListener implements ActionListener {

         private MouseEvent event;

         public synchronized void setEvent(MouseEvent e) {
             event = e;
         }

         public synchronized void actionPerformed(ActionEvent ae) {
             if (event != null) {            
                 updateMouseMoved(event);
             }
         }
     }

     /**
      * The real mouseMoved call, called when mouseMoved is called and,
      * if there is a mouse timer interval set, that interval time has
      * passed.
      *
      * @return the consumeEvents setting of the mouse event concerns
      * an OMGraphic, false if it didn't.
      */
     protected boolean updateMouseMoved(MouseEvent e) {
         boolean ret = false;
         OMGraphic omg = getGeometryUnder(e);
         RepastGeometryOfInterest goi = getMovementInterest();

         if (omg != null && grp != null) {

             // This gets called if the goi is new or if the goi
             // refers to a different OMGraphic as previously noted.
             if (goi == null || !goi.appliesTo(omg)) {

                 if (goi != null) {
                     mouseNotOver(goi.getGeometry());
                 }

                 goi = new RepastGeometryOfInterest(omg, e, this);
                 setMovementInterest(goi);
                 setNoTimerOverOMGraphic(!omg.shouldRenderFill());
                 ret = mouseOver(omg, e);
             }

         } else {
             if (goi != null) {
                 mouseNotOver(goi.getGeometry());
                 setMovementInterest(null);
             }
             ret = mouseOver(e);
         }

         return ret && consumeEvents;
     }

     /**
      * Handle notification that another layer consumed a mouse moved
      * event.  Sets movement interest to null.
      */
     public void mouseMoved() {
         RepastGeometryOfInterest goi = getMovementInterest();
         if (goi != null) {
             mouseNotOver(goi.getGeometry());
             setMovementInterest(null);
         }
     }

     /**
      * Handle a left-click on the map.  Does nothing by default.
      * @return false
      */
     public boolean leftClick(MouseEvent me) {
         if (DEBUG) {
             Debug.output("leftClick(MAP) at " + me.getX() + ", " + me.getY());
         }

         if (grp != null && grp.receivesMapEvents() && me instanceof MapMouseEvent) {
             return grp.leftClick((MapMouseEvent) me);
         }

         return false;
     }

     /**
      * Handle a left-click on an OMGraphic. Does nothing by default.
      * @return true
      */
     public boolean leftClick(OMGraphic omg, MouseEvent me) {
         if (DEBUG) {
             Debug.output("leftClick(" + omg.getClass().getName() + ") at " + 
                          me.getX() + ", " + me.getY());
         }
      //  if (grp != null && grp.receivesMapEvents() && me instanceof MapMouseEvent && omg != null) {
       //      return grp.leftClick(omg, (MapMouseEvent) me);
       //  }
         return false;
     }

     /**
      * Notification that the user clicked on something else other than
      * the provided OMGraphic that was previously left-clicked on.
      * Calls deselect(omg).
      * @return false
      */
     public boolean leftClickOff(OMGraphic omg, MouseEvent me) {
         if (DEBUG) {
             Debug.output("leftClickOff(" + omg.getClass().getName() + ") at " + 
                          me.getX() + ", " + me.getY());
         }

         deselect(omg);

         return false;
     }

     /**
      * Notification that the map was right-clicked on.
      * @return false
      */
     public boolean rightClick(MouseEvent me) {
         if (DEBUG) {
             Debug.output("rightClick(MAP) at " + me.getX() + ", " + me.getY());
         }

         if (me instanceof MapMouseEvent) {
             return displayPopup(grp.getItemsForMapMenu((MapMouseEvent)me), me);
         }

         return false;
     }

     /**
      * Notification that an OMGraphic was right-clicked on.
      * @return true
      */
     public boolean rightClick(OMGraphic omg, MouseEvent me) {
         if (DEBUG) {
             Debug.output("rightClick(" + omg.getClass().getName() + ") at " + 
                          me.getX() + ", " + me.getY());
         }

         return displayPopup(grp.getItemsForOMGraphicMenu(omg), me);
     }

     /**
      * Create a popup menu from GRP requests, over the mouse event
      * location.
      * @return true if popup was presented, false if not.
      */
     protected boolean displayPopup(List contents, MouseEvent me) {
         if (DEBUG) {
             Debug.output("displayPopup(" + contents + ") " + me);
         }
         if (contents != null && contents.size() > 0) {
             JPopupMenu jpm = new JPopupMenu();
             Iterator it = contents.iterator();
             while (it.hasNext()) {
                 Object obj = it.next();
                 if (obj instanceof java.awt.Component) {
                     jpm.add((java.awt.Component)obj);
                 }
             }
             jpm.show((java.awt.Component)me.getSource(), me.getX(), me.getY());
             return true;
         }
         return false;
     }

     /**
      * Notification that the user clicked on something else other than
      * the provided OMGraphic that was previously right-clicked on.
      * @return false
      */
     public boolean rightClickOff(OMGraphic omg, MouseEvent me) {
         if (DEBUG) {
             Debug.output("rightClickOff(" + omg.getClass().getName() + ") at " + 
                          me.getX() + ", " + me.getY());
         }

         return false;
     }

     /**
      * Notification that the mouse is not over an OMGraphic, but over
      * the map at some location.
      * @return false
      */
     public boolean mouseOver(MouseEvent me) {
         if (DEBUG) {
             Debug.output("mouseOver(MAP) at " + me.getX() + ", " + me.getY());
         }
         if (grp != null && grp.receivesMapEvents() && me instanceof MapMouseEvent) {
             return grp.mouseOver((MapMouseEvent) me);
         }

         return false;
     }

     /**
      * Notification that the mouse is over an OMGraphic.  Makes all
      * the highlight calls.
      * @return true
      */
     public boolean mouseOver(OMGraphic omg, MouseEvent me) {
         if (DEBUG) {
             Debug.output("mouseOver(" + omg.getClass().getName() + ") at " + 
                          me.getX() + ", " + me.getY());
         }

         if (grp != null) {
             handleToolTip(grp.getToolTipTextFor(omg));
             handleInfoLine(grp.getInfoText(omg));
             if (grp.isHighlightable(omg)) {
                 grp.highlight(omg);
             }
         }
         return true;
     }

     /**
      * Given a tool tip String, use the layer to get it displayed.
      */
     protected void handleToolTip(String tip) {
         if (lastToolTip == tip) {
             return;
         }
         lastToolTip = tip;
         if (layer != null) {
             if (lastToolTip != null) {
                 layer.fireRequestToolTip(lastToolTip);
             } else {
                 layer.fireHideToolTip();
             }
         }
     }

     /**
      * Given an information line, use the layer to get it displayed on
      * the InformationDelegator.
      */
     protected void handleInfoLine(String line) {
         if (layer != null) {
             layer.fireRequestInfoLine((line==null)?"":line);
         }
     }

     /**
      * Notification that the mouse has moved off of an OMGraphic.
      */
     public boolean mouseNotOver(OMGraphic omg) {
         if (DEBUG) {
             Debug.output("mouseNotOver(" + omg.getClass().getName() + ")");
         }

         if (grp != null) {
             grp.unhighlight(omg);
         }
         handleToolTip(null);
         handleInfoLine(null);
         return false;
     }

     /**
      * Notify the GRP that the OMGraphic has been selected.  Wraps
      * the OMGraphic in an OMGraphicList.
      */
     public void select(OMGraphic omg) {
         if (grp != null && grp.isSelectable(omg)) {
             OMGraphicList omgl = new OMGraphicList();
             omgl.add(omg);
             grp.select(omgl);
         }
     }

     /**
      * Notify the GRP that the OMGraphic has been deselected.  Wraps
      * the OMGraphic in an OMGraphicList.
      */
     public void deselect(OMGraphic omg) {
         if (grp != null && grp.isSelectable(omg)) {
             OMGraphicList omgl = new OMGraphicList();
             omgl.add(omg);
             grp.deselect(omgl);
         }
     }

     /**
      * The last MouseEvent received, for later reference.
      */
     protected MouseEvent currentMouseEvent;

     /**
      * Set the last MouseEvent received.
      */
     protected void setCurrentMouseEvent(MouseEvent me) {
         currentMouseEvent = me;
     }

     /**
      * Get the last MouseEvent received.
      */
     public MouseEvent getCurrentMouseEvent() {
         return currentMouseEvent;
     }

     /**
      * Set the GestureResponsePolicy to notify of the mouse actions
      * over the layer's OMGraphicList.
      */
     public void setGRP(GestureResponsePolicy grp) {
         this.grp = grp;
     }

     /**
      * Get the GestureResponsePolicy that is being notified of the
      * mouse actions over the layer's OMGraphicList.
      */
     public GestureResponsePolicy getGRP() {
         return grp;
     }
 }
 /**
  * Helper class used to keep track of OMGraphics of interest.
  * Interest means that a MouseEvent that occured over an OMGraphic
  * that combined with another MouseEvent, may be interpreted as a
  * significant event.
  */
  class RepastGeometryOfInterest {
     OMGraphic omg;
     int button;
     boolean leftButton;
     RepastMapMouseInterpreter rmmi;
     /**
      * Create a Geometry of Interest with the OMGraphic and the
      * first mouse event.
      */
     public RepastGeometryOfInterest(OMGraphic geom, MouseEvent me) {
         omg = geom;
         button = getButton(me);
//         this.rmmi = rmmi;
   //      leftButton = this.rmmi.isLeftMouseButton(me);
     }
     
     public RepastGeometryOfInterest(OMGraphic geom, MouseEvent me, RepastMapMouseInterpreter rmmi) {
         omg = geom;
         button = getButton(me);
         this.rmmi = rmmi;
         leftButton = this.rmmi.isLeftMouseButton(me);
     }

     /**
      * A check to see if an OMGraphic is the same as the one of
      * interest.
      */
     public boolean appliesTo(OMGraphic geom) {
         return (geom == omg);
     }

     /**
      * A check to see if a mouse event that is occuring over an
      * OMGraphic is infact occuring over the one of interest, and
      * with the same mouse button.
      */
     public boolean appliesTo(OMGraphic geom, MouseEvent me) {
         return (geom == omg && sameButton(me));
     }

     /**
      * A check to see if the current mouse event concerns the same
      * mouse button as the original.
      */
     public boolean sameButton(MouseEvent me) {
         return button == getButton(me);
     }
     
     /**
      * Return the OMGraphic of interest.
      */
     public OMGraphic getGeometry() {
         return omg;
     }

     /**
      * Return the button that caused the interest.
      */
     public int getButton() {
         return button;
     }

     /**
      * Utility method to get around MouseEvent.getButton 1.4
      * requirement.
      */
     protected int getButton(MouseEvent me) {
         // jdk 1.4 version
         // return me.getButton();

         // jdk 1.3 version Don't know if the numbers are the same
         // as in me.getButton, shouldn't make a difference.
         if (SwingUtilities.isLeftMouseButton(me)) {
             return 0;
         } else if (SwingUtilities.isRightMouseButton(me) || 
                    me.isControlDown()) {
             return 1;
         } else {
             return 2;
         }
     }

     /**
      * Return if the current button is the left one.
      */
     public boolean isLeftButton() {
         return leftButton;
     }
 }
