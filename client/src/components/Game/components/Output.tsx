import { Button } from "@chakra-ui/react";
import { useState } from "react";
import { useGlobalState } from "../GlobalStateProvider";
interface TestCase {
  params: string;
  expected: string;
}

// output box showing three test cases and outputs
export default function Output() {
  const [activeCase, setActiveCase] = useState<string | null>("Case 1");
  const state = useGlobalState();
  const [test1, test2, test3] = Object.entries(state.problem.tests)
    .slice(0, 3)
    .map(([_, test]) => test);
  const paramNames = state.problem.params;

  // change active case
  const handleButtonClick = (caseName: string) => {
    setActiveCase(caseName);
  };

  // handle output for each input
  function InputOutput(caseData: TestCase, index: number) {
    const { params, expected } = caseData;
    const paramList = params.includes("[")
      ? params.split(/(?<=\]),\s*/)
      : params.split(",");

    // get output for each test case
    function getOutput(index: number) {
      const outputs = Object.entries(state.output).slice(0, 3);

      switch (index) {
        case 0:
          return outputs[0]?.[1]?.actual;
        case 1:
          return outputs[1]?.[1]?.actual;
        case 2:
          return outputs[2]?.[1]?.actual;
        default:
          return null;
      }
    }

    // get standard output, if applicable
    function getStdOutput(index: number) {
      const stdOutput = state.stdOutput;
      if (state.outputType === "bug") {
        const bugOutput = stdOutput.join("\n");
        return bugOutput.split("\n").map((line, index) => (
          <p
            key={index}
            className="font-courier text-sm font-semibold"
            style={{ whiteSpace: "pre-wrap" }}
          >
            {line}
          </p>
        ));
      } else {
        return stdOutput[index];
      }
    }

    if (!params || !expected) {
      return <div>Loading output section...</div>;
    }

    return (
      <div className="flex flex-col w-full px-2 gap-4">
        {/* Input Section */}
        <div className="flex flex-col">
          <p className="font-montserrat text-xs font-semibold pb-1 text-gray-400">
            Input
          </p>
          <div className="flex flex-col">
            {paramNames.map((param: string, paramIndex: number) => (
              <div key={paramIndex} className="flex flex-col pb-2">
                <div className="flex flex-col p-2 bg-zinc-700 rounded-lg">
                  <p className="text-xs pb-2 text-gray-400">{param}:</p>
                  <p className="font-courier text-sm font-semibold">
                    {paramList[paramIndex]}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Standard Output Section */}
        <div className="flex flex-col">
          <p className="font-montserrat text-xs font-semibold pb-1 text-gray-400">
            Stdout
          </p>
          <div className="flex flex-col pb-2">
            <div className="flex flex-col p-3 bg-zinc-700 rounded-lg">
              <p className="font-courier text-sm font-semibold">
                {getStdOutput(index)}
              </p>
            </div>
          </div>
        </div>

        {/* Output Section */}
        <div className="flex flex-col">
          <p className="font-montserrat text-xs font-semibold pb-1 text-gray-400">
            Output
          </p>
          <div className="flex flex-col pb-2">
            <div className="flex flex-col p-3 bg-zinc-700 rounded-lg">
              <p className="font-courier text-sm font-semibold">
                {getOutput(index)}
              </p>
            </div>
          </div>
        </div>

        {/* Expected Section */}
        <div className="flex flex-col">
          <p className="font-montserrat text-xs font-semibold pb-1 text-gray-400">
            Expected
          </p>
          <div className="flex flex-col pb-2">
            <div className="flex flex-col p-3 bg-zinc-700 rounded-lg">
              <p className="font-courier text-sm font-semibold">{expected}</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full h-full flex flex-col p-4 overflow-auto">
      <div className="flex flex-row gap-2 pb-4">
        <Button
          size="sm"
          bg={activeCase === "Case 1" ? "#3C3C3C" : "#1E1E1E"}
          _hover={
            activeCase === "Case 1" ? { bg: "#3C3C3C" } : { bg: "gray.700" }
          }
          textColor="white"
          onClick={() => handleButtonClick("Case 1")}
        >
          Case 1
        </Button>
        <Button
          size="sm"
          bg={activeCase === "Case 2" ? "#3C3C3C" : "#1E1E1E"}
          _hover={
            activeCase === "Case 2" ? { bg: "#3C3C3C" } : { bg: "gray.700" }
          }
          textColor="white"
          onClick={() => handleButtonClick("Case 2")}
        >
          Case 2
        </Button>
        <Button
          size="sm"
          bg={activeCase === "Case 3" ? "#3C3C3C" : "#1E1E1E"}
          _hover={
            activeCase === "Case 3" ? { bg: "#3C3C3C" } : { bg: "gray.700" }
          }
          textColor="white"
          onClick={() => handleButtonClick("Case 3")}
        >
          Case 3
        </Button>
      </div>
      {activeCase === "Case 1" && InputOutput(test1, 0)}
      {activeCase === "Case 2" && InputOutput(test2, 1)}
      {activeCase === "Case 3" && InputOutput(test3, 2)}
    </div>
  );
}
