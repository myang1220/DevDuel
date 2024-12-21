import { queryAPI } from "../../../api.ts";
import { useGlobalState, useGlobalDispatch } from "../GlobalStateProvider";
import { useEffect, useState } from "react";

interface ProblemProps {
  problemID: string;
}

// shows problem description and examples using problemID
export default function Problem({ problemID }: ProblemProps) {
  const state = useGlobalState();
  const dispatch = useGlobalDispatch();
  const [problem, setProblem] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  // get problem information using getproblem endpoint
  async function fetchProblem() {
    console.log(problemID);
    try {
      const response = await queryAPI("getproblem", {
        problemID: problemID,
      });
      if (response.response_type === "success") {
        const fetchedProblem = response.body[0];
        dispatch({ type: "SET_PROBLEM", payload: fetchedProblem });
        setProblem(fetchedProblem);
      } else {
        console.error("Failed to fetch problem");
      }
    } catch (error) {
      console.error("Error fetching problem:", error);
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    fetchProblem();
  }, []);

  if (isLoading) {
    return <div>Loading problem...</div>;
  }

  if (!problem) {
    return <div>Failed to load problem.</div>;
  }

  const { name, description, tests, params } = state.problem;

  return (
    <div className="p-2">
      <h1 className="flex justify-center text-3xl font-bold py-2 font-courier">
        {name}
      </h1>
      <div className="flex flex-col gap-4 pb-8 text-gray-200 text-sm">
        {description.split("\\n").map((line, index) => (
          <p key={index}>
            {line.split("\\t").map((segment, i) => (
              <span key={i} style={{ paddingLeft: i > 0 ? "1em" : "0" }}>
                {segment}
              </span>
            ))}
          </p>
        ))}
      </div>
      <div>
        {Object.entries(tests)
          .slice(0, 3)
          .map(([key, test], index) => {
            const paramList = test.params.includes("[")
              ? test.params.split(/(?<=\]),\s*/)
              : test.params.split(",");
            const formattedParams = paramList.map(
              (param, i) => `${params[i]} = ${param}`
            );

            return (
              <div key={key} className="flex flex-col gap-4 text-sm">
                <h3 className="font-semibold">Example {index + 1}</h3>
                <div className="flex flex-row pb-4">
                  <div className="w-[2px] bg-gray-600"></div>
                  <div className="font-courier text-xs">
                    <p className="pl-6 text-gray-400">
                      <strong className="text-gray-200">Input: </strong>
                      {formattedParams.join(", ")}
                    </p>
                    <p className="pl-6 text-gray-400">
                      <strong className="text-gray-200">Output: </strong>
                      {test.expected}
                    </p>
                  </div>
                </div>
              </div>
            );
          })}
      </div>
    </div>
  );
}
