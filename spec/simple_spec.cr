require "./spec_helper"

describe Protocr::Spec::Simple do
  it "decodes simple" do
    thing = Protocr::Spec::Simple.from_protobuf data("Simple.1")
    thing.has_text?.should be_true
    thing.text.should eq "Hello, world!"
  end

  it "decodes empty" do
    thing = Protocr::Spec::Simple.from_protobuf data("Simple.empty")
    thing.has_text?.should be_false
    thing.text.should eq ""
  end
end
