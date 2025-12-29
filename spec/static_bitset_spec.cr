require "spec"
require "../src/protocr"

describe Protocr::StaticBitset do
  it "works" do
    bs = Protocr::StaticBitset(2).new

    # Verify all bits start as zero
    0.upto 15 do |i|
      bs.test(i).should eq false
    end
    # std::cout << "All bits initialized to zero: PASS\n";

    # Set some bits
    bs.set(0, true)
    bs.set(7, true)
    bs.set(8, true)
    bs.set(15, true)

    # Verify set bits
    bs.test(0).should eq true
    bs.test(7).should eq true
    bs.test(8).should eq true
    bs.test(15).should eq true
    bs.test(1).should eq false
    bs.test(14).should eq false

    # Clear a bit
    bs.set(7, false);
    bs.test(7).should eq false

    sizeof(Protocr::StaticBitset(1)).should eq 1
    sizeof(Protocr::StaticBitset(2)).should eq 2
    sizeof(Protocr::StaticBitset(8)).should eq 8
  end
end
