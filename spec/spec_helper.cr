require "spec"
require "../src/protocr"
require "./gen/*"

def data(name : String)
  buf = IO::Memory.new
  File.open("spec/binary/#{name}.binpb", "r") do |f|
    IO.copy f, buf
  end
  buf.rewind
  buf
end
