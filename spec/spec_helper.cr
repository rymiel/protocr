require "spec"
require "../src/protocr"
require "./gen/*"

def data(name : String) : Bytes
  File.open("spec/binary/#{name}.binpb", "r") do |f|
    f.getb_to_end
  end
end
