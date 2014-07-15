
module Loom

  class SignalHandler
    def initialize(signal)
      @interruptable = false
      @enqueued     = []
      trap(signal) do
        if @interruptable
          #log.info 'Gracefully shutting down provisioner...'
          #print "Gracefully shutting down provisioner...\n"
          exit
        else
          @enqueued.push(signal)
        end
      end
    end

    # If this is called with a block then the block will be run with
    # the signal temporarily ignored. Without the block, we'll just set
    # the flag and the caller can call `allow_interruptions` themselves.
    def dont_interrupt
      @interruptable = false
      @enqueued     = []
      if block_given?
        yield
        allow_interruptions
      end
    end

    def allow_interruptions
      @interruptable = true
      # Send the temporarily ignored signals to ourself
      # see http://www.ruby-doc.org/core/classes/Process.html#M001286
      #@enqueued.each { |signal| Process.kill(signal, 0) }
      @enqueued.each do |signal|
        #print "*** sending queued #{signal} to process #{Process.pid} ***\n"
        Process.kill(signal, Process.pid)
        # there is a race condition here if the calling process immediately enters dont_interrupt again
        # allow time for signal handler to process, since this is only used for terminating signals
        sleep 1
      end
    end
  end

end

