require "./spec_helper"

include Protocr::Spec

describe Protocr do
  it "decodes simple" do
    thing = Simple.from_protobuf data("Simple.1")
    thing.has_text?.should be_true
    thing.text.should eq "Hello, world!"
  end

  it "decodes simple empty" do
    thing = Simple.from_protobuf data("Simple.empty")
    thing.has_text?.should be_false
    thing.text.should eq ""
  end

  it "decodes basic" do
    thing = Basic.from_protobuf data("Basic.1")
    thing.has_string?.should be_true
    thing.string.should eq "String"
    thing.has_bytes?.should be_true
    thing.bytes.should eq "0010203040".hexbytes
    thing.has_uint64?.should be_true
    thing.uint64.should eq 42949672950u64
    thing.has_uint32?.should be_true
    thing.uint32.should eq 4294967295u32
  end

  it "decodes basic partial" do
    thing = Basic.from_protobuf data("Basic.2")
    thing.has_string?.should be_true
    thing.string.should eq "String"
    thing.has_bytes?.should be_false
    thing.bytes.should be_empty
    thing.has_uint64?.should be_true
    thing.uint64.should eq 0u64
    thing.has_uint32?.should be_false
    thing.uint32.should eq 0u32
  end

  it "encodes simple" do
    thing = Simple.new
    thing.text = "Hello, world!"
    thing.to_protobuf.should eq data("Simple.1")
  end

  it "encodes simple empty" do
    thing = Simple.new
    thing.to_protobuf.should eq data("Simple.empty")
  end

  it "encodes basic" do
    thing = Basic.new
    thing.string = "String"
    thing.bytes = "0010203040".hexbytes
    thing.uint64 = 42949672950u64
    thing.uint32 = 4294967295u32
    thing.to_protobuf.should eq data("Basic.1")
  end

  it "encodes basic partial" do
    thing = Basic.new
    thing.string = "String"
    thing.uint64 = 0u64
    thing.to_protobuf.should eq data("Basic.2")
  end

  it "encodes basic to basic partial" do
    thing = Basic.from_protobuf data("Basic.1")
    thing.clear_bytes!
    thing.uint64 = 0u64
    thing.clear_uint32!
    thing.to_protobuf.should eq data("Basic.2")
  end
end
