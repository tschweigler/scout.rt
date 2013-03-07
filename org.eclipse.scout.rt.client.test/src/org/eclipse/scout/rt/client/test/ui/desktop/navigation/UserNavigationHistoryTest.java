/*******************************************************************************
 * Copyright (c) 2010,2012 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.client.test.ui.desktop.navigation;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ui.desktop.navigation.NavigationHistoryEvent;
import org.eclipse.scout.rt.client.ui.desktop.navigation.NavigationHistoryListener;
import org.eclipse.scout.rt.client.ui.desktop.navigation.internal.UserNavigationHistory;
import org.eclipse.scout.rt.shared.services.common.bookmark.Bookmark;
import org.eclipse.scout.rt.shared.services.common.bookmark.NodePageState;
import org.eclipse.scout.rt.shared.services.common.bookmark.TablePageState;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for {@link org.eclipse.scout.rt.client.ui.desktop.navigation.internal.UserNavigationHistory
 * UserNavigationHistory}
 */
public class UserNavigationHistoryTest {

  /**
   * Tests the initialization
   */
  @Test
  public void testInitialNavigationHistory() {
    UserNavigationHistory history = new UserNavigationHistory();
    Assert.assertEquals("Inithial history size should be 0.", 0, history.getSize());
    Assert.assertNull("Inithially history should contains no active bookmark.", history.getActiveBookmark());
    Assert.assertEquals("Initially there should be no menus.", 0, history.getMenus().length);
  }

  /**
   * Tests the history state after adding a test bookmark.
   */
  @Test
  public void testAddingBookmark() {
    UserNavigationHistory history = new UserNavigationHistory();
    Bookmark testBookmark = getTestBookmark();

    Bookmark addedBookmark = history.addStep(testBookmark);
    Assert.assertEquals(testBookmark, addedBookmark);
    Assert.assertEquals(1, history.getSize());
    Assert.assertEquals(0, history.getIndex());
    Assert.assertEquals("Added bookmark should be active.", testBookmark, history.getActiveBookmark());
    Assert.assertEquals("There should be one menu for the added bookmark", 1, history.getMenus().length);
    Assert.assertEquals("There should be no backward bookmarks.", 0, history.getBackwardBookmarks().size());
    Assert.assertEquals("There should be no forward bookmarks.", 0, history.getForwardBookmarks().size());
  }

  /**
   * Tests adding a null bookmark.
   */
  @Test
  public void testAddingNullBookmark() {
    UserNavigationHistory history = new UserNavigationHistory();
    history.addStep(null);
    Assert.assertEquals("Null Bookmark should not be added to history.", 0, history.getSize());
  }

  /**
   * Adding the a new bookmark removes forward bookmarks.
   */
  @Test
  public void testAddingBookmarkIntermediate() throws ProcessingException {
    UserNavigationHistory history = getNavigationHistoryWitoutActivation();

    Bookmark bookmark0 = getTestBookmark(0L);
    Bookmark bookmark1 = getTestBookmark(1L);
    Bookmark bookmark2 = getTestBookmark(2L);
    history.addStep(bookmark0);
    history.addStep(bookmark1);
    history.addStep(bookmark2);
    final int initialHistorySize = 3;

    history.stepBackward();

    Bookmark bookmark4 = getTestBookmark();
    history.addStep(bookmark4);
    Assert.assertEquals(initialHistorySize, history.getSize());
    Assert.assertEquals(bookmark4, history.getActiveBookmark());
  }

  /**
   * Adding the same bookmark as the next one in the history does not remove the forward bookmarks.
   */
  @Test
  public void testAddingDuplicateBookmarkIntermediate() throws ProcessingException {
    UserNavigationHistory history = getNavigationHistoryWitoutActivation();

    Bookmark bookmark0 = getTestBookmark(0L);
    Bookmark bookmark1 = getTestBookmark(1L);
    Bookmark bookmark2 = getTestBookmark(2L);
    history.addStep(bookmark0);
    history.addStep(bookmark1);
    history.addStep(bookmark2);
    final int initialHistorySize = 3;

    history.stepBackward();
    history.stepBackward();
    history.addStep(bookmark1);

    Assert.assertEquals(initialHistorySize, history.getSize());
    Assert.assertEquals(bookmark1, history.getActiveBookmark());
  }

  /**
   * Tests the stepping one step backwards in the history.
   */
  @Test
  public void testSteppingBackward() throws ProcessingException {
    UserNavigationHistory history = getNavigationHistoryWitoutActivation();

    Bookmark bookmark0 = getTestBookmark(0L);
    Bookmark bookmark1 = getTestBookmark(1L);
    Bookmark bookmark2 = getTestBookmark(2L);
    history.addStep(bookmark0);
    history.addStep(bookmark1);
    history.addStep(bookmark2);
    final int initialHistorySize = 3;

    history.stepBackward();
    Assert.assertEquals(bookmark1, history.getActiveBookmark());
    Assert.assertEquals(1, history.getIndex());
    Assert.assertEquals(initialHistorySize, history.getSize());

    Assert.assertTrue(history.hasForwardBookmarks());
    Assert.assertTrue(history.getForwardBookmarks().contains(bookmark2));
    Assert.assertEquals(1, history.getForwardBookmarks().size());

    Assert.assertTrue(history.getBackwardBookmarks().contains(bookmark0));
    Assert.assertEquals(1, history.getBackwardBookmarks().size());
  }

  /**
   * Tests, if adding still works, when an exception is thrown in stepping backwards.
   */
  @Test
  public void testAddingAfterSteppingBackwardsFails() throws ProcessingException {
    UserNavigationHistory history = getNavigationHistoryWitoutActivation();

    Bookmark bookmark0 = getTestBookmark(0L);
    Bookmark bookmark1 = getTestBookmark(1L);
    Bookmark bookmark2 = getTestBookmark(2L);
    history.addStep(bookmark0);
    history.addStep(bookmark1);

    try {
      history.stepBackward();
    }
    catch (ProcessingException e) {
      //nop
    }
    history.addStep(bookmark2);

    Assert.assertEquals(bookmark2, history.getActiveBookmark());
  }

  /**
   * Tests the stepping one step backwards in the history.
   */
  @Test
  public void testSteppingForward() throws ProcessingException {
    UserNavigationHistory history = getNavigationHistoryWitoutActivation();

    Bookmark bookmark0 = getTestBookmark(0L);
    Bookmark bookmark1 = getTestBookmark(1L);
    Bookmark bookmark2 = getTestBookmark(2L);
    history.addStep(bookmark0);
    history.addStep(bookmark1);
    history.addStep(bookmark2);
    final int initialHistorySize = 3;

    history.stepBackward();
    history.stepForward();
    Assert.assertEquals(bookmark2, history.getActiveBookmark());
    Assert.assertEquals(2, history.getIndex());
    Assert.assertEquals(initialHistorySize, history.getSize());

    Assert.assertFalse(history.hasForwardBookmarks());
    Assert.assertTrue(history.hasBackwardBookmarks());
    Assert.assertTrue(history.getBackwardBookmarks().contains(bookmark0));
    Assert.assertEquals(2, history.getBackwardBookmarks().size());
  }

  /**
   * Tests stepping to an existing bookmark.
   */
  @Test
  public void testSteppingToExistingBookmark() throws ProcessingException {
    UserNavigationHistory history = getNavigationHistoryWitoutActivation();

    Bookmark bookmark0 = getTestBookmark(0L);
    Bookmark bookmark1 = getTestBookmark(1L);
    Bookmark bookmark2 = getTestBookmark(2L);
    history.addStep(bookmark0);
    history.addStep(bookmark1);
    history.addStep(bookmark2);

    history.stepTo(bookmark0);
    Assert.assertEquals(bookmark0, history.getActiveBookmark());
  }

  /**
   * Tests, if the active bookmarks stays unchanged when stepping to an non-existing bookmark.
   */
  @Test
  public void testSteppingToNonExistingBookmark() throws ProcessingException {
    UserNavigationHistory history = getNavigationHistoryWitoutActivation();

    Bookmark bookmark0 = getTestBookmark(0L);
    Bookmark bookmark1 = getTestBookmark(1L);
    Bookmark bookmark2 = getTestBookmark(2L);
    history.addStep(bookmark0);
    history.addStep(bookmark1);
    history.addStep(bookmark2);

    history.stepTo(getTestBookmark());
    Assert.assertEquals(bookmark2, history.getActiveBookmark());
  }

  /**
   * @return a UserNavigationHistory with overridden activateBookmark to avoid using a client session and desktop
   */
  private UserNavigationHistory getNavigationHistoryWitoutActivation() {
    UserNavigationHistory history = new UserNavigationHistory() {
      @Override
      protected void activateBookmark(Bookmark b) throws ProcessingException {
        // nop
      }
    };
    return history;
  }

  /**
   * Tests the stepping one step forward in the history.
   * No exception should be thrown
   */
  @Test
  public void testSteppingForwardOnEmptyHistory() throws ProcessingException {
    UserNavigationHistory history = new UserNavigationHistory();
    history.stepForward();
    Assert.assertEquals(0, history.getSize());
  }

  /**
   * Tests the stepping one step backwards in the history, if no element is available.
   * No exception should be thrown
   */
  @Test
  public void testSteppingBackwardOnEmptyHistory() throws ProcessingException {
    UserNavigationHistory history = new UserNavigationHistory();
    history.stepBackward();
    Assert.assertEquals(0, history.getSize());
  }

  @Test
  public void testHistoryTruncation() {
    //TODO history size should be accessible
    UserNavigationHistory history = new UserNavigationHistory();
    final int historySize = 25;
    Bookmark testBookmark = null;
    for (int i = 0; i < historySize + 2; i++) {
      testBookmark = getTestBookmark(i);
      history.addStep(testBookmark);
    }

    Assert.assertEquals(historySize, history.getSize());
    Assert.assertEquals(historySize - 1, history.getIndex());
    Assert.assertEquals("Last added bookmark should be active.", testBookmark, history.getActiveBookmark());
    Assert.assertEquals("There should be backward bookmarks.", historySize - 1, history.getBackwardBookmarks().size());
    Assert.assertTrue(history.getForwardBookmarks().isEmpty());
    Assert.assertEquals("There should be one menu.", historySize, history.getMenus().length);
  }

  /**
   * Tests, if NavigationHistoryEvent can be received when a listener is added.
   */
  @Test
  public void testNavigationHistoryListener() {
    TestNavigationHistoryTracker tracker = new TestNavigationHistoryTracker();
    UserNavigationHistory history = new UserNavigationHistory();
    history.addNavigationHistoryListener(tracker);
    Assert.assertTrue(tracker.getEvents().isEmpty());
    history.addStep(getTestBookmark());
    List<NavigationHistoryEvent> events = tracker.getEvents();
    Assert.assertFalse(events.isEmpty());
    assertContainsEvent(events, NavigationHistoryEvent.TYPE_BOOKMARK_ADDED);
    assertContainsEvent(events, NavigationHistoryEvent.TYPE_CHANGED);
  }

  /**
   * Tests removing a NavigationHistoryListener works
   **/
  @Test
  public void testRemovingNavigationHistoryListener() {
    TestNavigationHistoryTracker tracker = new TestNavigationHistoryTracker();
    UserNavigationHistory history = new UserNavigationHistory();
    history.addNavigationHistoryListener(tracker);
    history.removeNavigationHistoryListener(tracker);
    Assert.assertTrue(tracker.getEvents().isEmpty());
    history.addStep(getTestBookmark());
    List<NavigationHistoryEvent> events = tracker.getEvents();
    Assert.assertTrue(events.isEmpty());
  }

  private static void assertContainsEvent(List<NavigationHistoryEvent> events, int type) {
    boolean found = false;
    for (NavigationHistoryEvent event : events) {
      if (type == event.getType()) {
        found = true;
        break;
      }
    }
    Assert.assertTrue(found);
  }

  private Bookmark getTestBookmark() {
    return getTestBookmark(0L);
  }

  private Bookmark getTestBookmark(long id) {
    Bookmark testBookmark = new Bookmark();
    testBookmark.setTitle("bm" + id);
    testBookmark.setId(id);
    return testBookmark;
  }

  /**
   * Tests adding a bookmark that is already the last element in the history.
   */
  @Test
  public void testAddingDuplicateBookmark() {
    UserNavigationHistory history = new UserNavigationHistory();
    Bookmark testBookmark = getTestBookmark();

    history.addStep(testBookmark);
    history.addStep(testBookmark);

    Assert.assertEquals(1, history.getSize());
    Assert.assertEquals(0, history.getIndex());
    Assert.assertEquals("Added bookmark should be active.", testBookmark, history.getActiveBookmark());
    Assert.assertEquals("There should be no backward bookmarks.", 0, history.getBackwardBookmarks().size());
    Assert.assertFalse("There should be no backward bookmarks.", history.hasBackwardBookmarks());
    Assert.assertEquals("There should be no forward bookmarks.", 0, history.getForwardBookmarks().size());
    Assert.assertFalse("There should be no forward bookmarks.", history.hasForwardBookmarks());
    Assert.assertEquals("There should be one menu.", 1, history.getMenus().length);
  }

  /**
   * Tests that a bookmark with different pathlength is not added.
   */
  @Test
  public void testAddingDuplicatesDifferentPathLength() {
    UserNavigationHistory history = new UserNavigationHistory();
    Bookmark testBookmark1 = getTestBookmark();
    Bookmark testBookmark2 = getTestBookmark();
    testBookmark2.addPathElement(new TablePageState());

    history.addStep(testBookmark1);
    history.addStep(testBookmark2);

    Assert.assertEquals(2, history.getSize());
  }

  /**
   * Tests that a bookmark with the same path is not added
   **/
  @Test
  public void testAddingDuplicatesSamePaths() {
    UserNavigationHistory history = new UserNavigationHistory();
    Bookmark testBookmark1 = getTestBookmark();
    Bookmark testBookmark2 = getTestBookmark();
    TablePageState tablePageState1 = new TablePageState();
    testBookmark1.addPathElement(tablePageState1);
    testBookmark2.addPathElement(tablePageState1);

    history.addStep(testBookmark1);
    history.addStep(testBookmark2);

    Assert.assertEquals(1, history.getSize());
  }

  /**
   * Tests that a bookmark with the same path, but different search form state is added.
   **/
  @Test
  public void testAddingDuplicatesPathsDifferentSearchFormState() {
    UserNavigationHistory history = new UserNavigationHistory();
    Bookmark testBookmark1 = getTestBookmark();
    Bookmark testBookmark2 = getTestBookmark();
    TablePageState tablePageState1 = new TablePageState();
    tablePageState1.setSearchFormState("test");
    TablePageState tablePageState2 = new TablePageState();
    tablePageState2.setSearchFormState("test2");
    testBookmark1.addPathElement(tablePageState1);
    testBookmark2.addPathElement(tablePageState2);

    history.addStep(testBookmark1);
    history.addStep(testBookmark2);

    Assert.assertEquals(2, history.getSize());
  }

  /**
   * Tests that a bookmark with different label is added.
   */
  @Test
  public void testAddingDuplicatesPathsDifferentLabel() {
    UserNavigationHistory history = new UserNavigationHistory();
    Bookmark testBookmark1 = getTestBookmark();
    Bookmark testBookmark2 = getTestBookmark();
    NodePageState tablePageState1 = new NodePageState();
    tablePageState1.setLabel("label1");
    NodePageState tablePageState2 = new NodePageState();
    tablePageState2.setLabel("label2");
    testBookmark1.addPathElement(tablePageState1);
    testBookmark2.addPathElement(tablePageState2);

    history.addStep(testBookmark1);
    history.addStep(testBookmark2);

    Assert.assertEquals(2, history.getSize());
  }

  /**
   * Tests that a bookmark with the same label is not added
   **/
  @Test
  public void testAddingDuplicatesPathsSameLabel() {
    UserNavigationHistory history = new UserNavigationHistory();
    Bookmark testBookmark1 = getTestBookmark();
    Bookmark testBookmark2 = getTestBookmark();
    NodePageState tablePageState1 = new NodePageState();
    tablePageState1.setLabel("label1");
    NodePageState tablePageState2 = new NodePageState();
    tablePageState2.setLabel("label1");
    testBookmark1.addPathElement(tablePageState1);
    testBookmark2.addPathElement(tablePageState2);

    history.addStep(testBookmark1);
    history.addStep(testBookmark2);

    Assert.assertEquals(1, history.getSize());
  }

  /**
   * Helper Class for tracking events. Stores the received events in a list.
   */
  static class TestNavigationHistoryTracker implements NavigationHistoryListener {

    private List<NavigationHistoryEvent> m_events = new LinkedList<NavigationHistoryEvent>();

    /**
     * @return the stored events
     */
    public List<NavigationHistoryEvent> getEvents() {
      return m_events;
    }

    /**
     * stores the navigation events in a list
     */
    @Override
    public void navigationChanged(NavigationHistoryEvent e) {
      m_events.add(e);
    }

  }

}
