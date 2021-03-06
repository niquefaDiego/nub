/******************************************************************************************
 * nub
 * Copyright (c) 2019 Universidad Nacional de Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ******************************************************************************************/

package nub.timing;

/**
 * Tasks are (non)recurrent, (non)concurrent (see {@link #isRecurrent()}
 * and {@link #isConcurrent()} resp.) callbacks defined by overridden
 * {@link #execute()}.
 * <p>
 * Note that concurrence of the task execution (which requires a separate
 * execution thread) should be implemented by derived classes, i.e., this
 * class just implements the task sequential api.
 * <p>
 * A task runs (see {@link #run(long)}) at a certain {@link #period()}
 * which define the interval duration between two consecutive executions
 * (see also {@link #frequency()}). Do not use the task for drawing
 * since it will not necessarily run every frame.
 * <p>
 * Call {@link TimingHandler#unregisterTask(Task)} to cancel the task.
 */
abstract public class Task {
  protected TimingHandler _timingHandler;
  protected boolean _active;
  protected boolean _recurrence;
  protected boolean _concurrence;
  protected long _counter;
  protected long _period;
  protected long _startTime;

  /**
   * Constructs a sequential recurrent task with a {@link #period()} of 40ms
   * (i.e., a {@link #frequency()} of 25 Hz).
   */
  public Task(TimingHandler timingHandler) {
    _timingHandler = timingHandler;
    _timingHandler.registerTask(this);
    _recurrence = true;
    _period = 40;
  }

  /**
   * Callback method which should be implemented by derived classes.
   * <p>
   * The task will be executed (see {@link #run(long)}) at a certain
   * {@link #period()}. Do not implement this method for drawing
   * since it will not necessarily be executed every frame.
   */
  abstract public void execute();

  /**
   * Executes the callback method defined by the {@link #execute()}.
   *
   * <b>Note:</b> This method is called by the timing handler
   * (see {@link nub.timing.TimingHandler#handle()}).
   */
  protected boolean _execute() {
    boolean result = false;
    if (_active) {
      long elapsedTime = System.currentTimeMillis() - _startTime;
      float timePerFrame = (1 / _timingHandler.frameRate()) * 1000;
      long threshold = _counter * _period;
      if (threshold >= elapsedTime) {
        long diff = elapsedTime + (long) timePerFrame - threshold;
        if (diff >= 0)
          if ((threshold - elapsedTime) < diff)
            result = true;
      } else
        result = true;
      if (result)
        _counter++;
    }
    if (result) {
      execute();
      if (!_recurrence)
        _active = false;
    }
    return result;
  }

  /**
   * Sets the task {@link #period()} in milliseconds and call {@link #run()}.
   * If task {@link #isRecurrent()} the {@link #execute()} method
   * will be invoked recurrently every {@link #period()} milliseconds;
   * otherwise it will be invoked once after a {@link #period()} delay
   * milliseconds.
   *
   * @see #run()
   * @see #isRecurrent()
   * @see #enableRecurrence(boolean)
   * @see #period()
   * @see #setPeriod(long)
   */
  public void run(long period) {
    setPeriod(period);
    run();
  }

  /**
   * Runs the task according to {@link #period()}. The timer may be scheduled for
   * repeated fixed-rate execution according to {@link #isRecurrent()}.
   */
  public void run() {
    _active = true;
    _counter = 1;
    _startTime = System.currentTimeMillis();
  }

  /**
   * Deactivates the task. See {@link #isActive()}.
   */
  public void stop() {
    _active = false;
  }

  /**
   * Calls {@link #stop()} if the task {@link #isActive()}, and {@link #run()} otherwise.
   */
  public void toggle() {
    if (isActive())
      stop();
    else
      run();
  }

  /**
   * Tells whether or not the timer is active.
   */
  public boolean isActive() {
    return _active;
  }

  /**
   * Returns the task period (the task execution interval duration) in milliseconds.
   * Non-recurrent tasks execute only once taking this value as their execution delay.
   * Default value is 40 ms.
   *
   * @see #setPeriod(long)
   * @see #frequency()
   */
  public long period() {
    return _period;
  }

  /**
   * Same as {@code setPeriod(period() + delta)}.
   *
   * @see #setPeriod(long)
   * @see #setFrequency(float)
   */
  public void increasePeriod(long delta) {
    setPeriod(period() + delta);
  }

  /**
   * Defines the task {@link #period()} in milliseconds.
   *
   * @see #period()
   * @see #setFrequency(float)
   */
  public void setPeriod(long period) {
    if (period <= 0) {
      System.out.println("Task period not set as it should have non-negative value");
      return;
    }
    _period = period;
    float target = frequency();
    if (_timingHandler.frameRate() < target) {
      System.out.println("Warning: Your task period of " + period + " ms requires at least a " + target + " Hz frameRate, " +
          "but currently it just achieves " + _timingHandler.frameRate() + " Hz." + '\n' + "Either set a period of at least "
          + 1000 / _timingHandler.frameRate() + " ms or call enableConcurrence() to execute the task concurrently.");
    }
  }

  /**
   * Defines the task {@link #frequency()} in Hz.
   *
   * @see #frequency()
   * @see #setPeriod(long)
   */
  public void setFrequency(float frequency) {
    setPeriod((long) (1000 / frequency));
  }

  /**
   * Returns the task execution frequency in milliseconds. Default value is 25 Hz.
   *
   * @see #setFrequency(float)
   * @see #period()
   */
  public float frequency() {
    return 1000 / _period;
  }

  /**
   * Same as {@code enableRecurrence(true)}.
   *
   * @see #disableRecurrence()
   * @see #enableRecurrence(boolean)
   * @see #isRecurrent()
   * @see #enableConcurrence(boolean)
   */
  public void enableRecurrence() {
    enableRecurrence(true);
  }

  /**
   * Same as {@code enableRecurrence(false)}.
   *
   * @see #enableRecurrence()
   * @see #enableRecurrence(boolean)
   * @see #isRecurrent()
   * @see #enableConcurrence(boolean)
   */
  public void disableRecurrence() {
    enableRecurrence(false);
  }

  /**
   * Enables or (disables) the task recurrence according to {@code enable}.
   * Non-recurrent tasks execute only once using {@link #period()} as their execution delay.
   *
   * @see #isRecurrent()
   * @see #enableRecurrence()
   * @see #disableRecurrence()
   * @see #enableConcurrence(boolean)
   */
  public void enableRecurrence(boolean enable) {
    boolean isActive = isActive();
    stop();
    _recurrence = enable;
    if (isActive)
      run();
  }

  /**
   * Returns whether or not the task is scheduled to be executed recurrently.
   * <p>
   * A recurrent task (see {@link #isRecurrent()}) is periodically executed
   * at fixed duration time intervals (see {@link #period()} and
   * {@link #setPeriod(long)}). A non-recurrent task will only be executed once
   * just after a delay of {@link #period()} ms.
   *
   * Tasks are recurrent by default, see {@link #Task(TimingHandler)}.
   *
   * @see #enableRecurrence(boolean)
   * @see #isConcurrent()
   */
  public boolean isRecurrent() {
    return _recurrence;
  }

  /**
   * Same as {@code enableConcurrence(true)}.
   *
   * @see #disableConcurrence()
   * @see #enableConcurrence(boolean)
   * @see #isConcurrent()
   * @see #enableRecurrence(boolean)
   */
  public void enableConcurrence() {
    enableConcurrence(true);
  }

  /**
   * Same as {@code enableConcurrence(false)}.
   *
   * @see #enableConcurrence()
   * @see #enableConcurrence(boolean)
   * @see #isConcurrent()
   * @see #enableRecurrence(boolean)
   */
  public void disableConcurrence() {
    enableConcurrence(false);
  }

  /**
   * Enables or (disables) the task concurrence according to {@code enable}.
   * Task concurrence should be implemented by derived classes.
   *
   * @see #isConcurrent()
   * @see #enableConcurrence()
   * @see #disableConcurrence()
   * @see #enableRecurrence(boolean)
   */
  public void enableConcurrence(boolean enable) {
    if (enable)
      System.out.println("Task can't be made concurrent. Concurrence should be implemented by derived classes.");
  }

  /**
   * Returns {@code true} if the task is concurrent, i.e., if it runs in parallel, and
   * {@code false} otherwise.
   * <p>
   * Task concurrence should be implemented by derived classes.
   *
   * @see #enableConcurrence(boolean)
   * @see #isRecurrent()
   */
  public boolean isConcurrent() {
    return _concurrence;
  }
}
