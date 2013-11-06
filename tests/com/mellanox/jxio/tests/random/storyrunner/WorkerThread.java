/*
 * * Copyright (C) 2013 Mellanox Technologies** Licensed under the Apache License, Version 2.0 (the "License");* you may
 * not use this file except in compliance with the License.* You may obtain a copy of the License at:**
 * http://www.apache.org/licenses/LICENSE-2.0** Unless required by applicable law or agreed to in writing, software*
 * distributed under the License is distributed on an "AS IS" BASIS,* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,*
 * either express or implied. See the License for the specific language* governing permissions and limitations under the
 * License.*
 */
package com.mellanox.jxio.tests.random.storyrunner;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.EventQueueHandler;
import com.mellanox.jxio.tests.random.storyrunner.TimerList.Timer;

public class WorkerThread implements Runnable {

	private final static Log LOG = LogFactory.getLog(WorkerThread.class.getSimpleName());

	public interface QueueAction {
		public void doAction(WorkerThread workerThread);
	}

	private final TimerList                  timers;
	private final EventQueueHandler          eqh;
	private final BlockingQueue<QueueAction> actions;

	public WorkerThread() {
		super();
		this.eqh = new EventQueueHandler();
		this.timers = new TimerList();
		this.actions = new LinkedBlockingQueue<QueueAction>();
		LOG.debug("new " + this.toString() + " done");
	}

	public String toString() {
		return "WorkerThread(id=" + Thread.currentThread().getId() + ")";
	}

	// add more action into this worker thread
	// an action can be an JXIO Client or Server, or other actions which should run on the EQH thread
	public void addWorkAction(QueueAction action) {
		LOG.debug(this.toString() + " addWorkAction(" + action.toString() + ")");
		this.actions.add(action);
		this.wakeup();
	}

	// start this timer by adding to list
	public void start(Timer timer) {
		if (timers.start(timer) == true) {
			wakeup();
		}
	}

	// stop this timer event
	public void stop(Timer timer) {
		timers.stop(timer);
	}

	public EventQueueHandler getEQH() {
		return eqh;
	}

	public void run() {

		while (true) {

			// check for pending actions in queue
			while (!this.actions.isEmpty()) {
				QueueAction action = this.actions.remove();
				LOG.debug(toString() + " handling action(=" + action.toString() + ")");
				action.doAction(this);
			}

			// block for JXIO events or timer list duration
			eqh.runEventLoop(-1, timers.getWaitDuration());

			// check Timers
			timers.checkForTimeOuts();
		}
	}

	// wakeup the thread's internal loop so it can check it's incoming event queue
	private void wakeup() {
		LOG.debug(toString() + " waking up...");
		eqh.breakEventLoop();
	}
}