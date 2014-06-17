
module Loom

  class SignalHandler
    def initialize(signal)
      @interruptable = false
      @enqueued     = []
      trap(signal) do
        if @interruptable
          #log.info 'Gracefully shutting down provisioner...'
          puts 'Gracefully shutting down provisioner...'
          exit 0
        else
          puts "queueing signal #{signal} until task complete"
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
      @enqueued.each { |signal| Process.kill(signal, 0) }
    end
  end

end

