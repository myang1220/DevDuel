import { useState } from "react";
import CodeReview from "./CodeReview";

type CodePanelProps = {
  name: string;
  numSolved: string;
  code: string;
  problemID: string;
};

// individual panels for each problem
export default function CodePanel({
  name,
  numSolved,
  code,
  problemID,
}: CodePanelProps) {
  const [reviewingCode, setReviewingCode] = useState(false);

  return (
    <div className="relative font-courier text-gray-500 font-bold rounded-xl shadow-2xl w-1/5 bg-gray-300 p-[1px]">
      <div className="flex flex-col h-full w-full bg-black py-6 rounded-xl">
        <h3 className="flex justify-center pb-8 px-4 text-lg text-gray-300 text-center break-words">
          {name}
        </h3>

        <div className="flex flex-col align-left px-4 gap-4 pb-4 text-sm font-normal">
          <p>
            <b className="font-bold text-gray-400">Number Solved: </b>
            {numSolved}
          </p>
        </div>

        <div className="flex justify-center mt-auto">
          <button className="relative font-courier text-gray-400 font-bold rounded-xl shadow-lg w-1/2 bg-gray-300 p-[1px] hover:shadow-xl">
            <div
              className="h-full w-full bg-stone-800 py-3 rounded-xl hover:bg-gray-700"
              onClick={() => setReviewingCode(true)}
            >
              Review
            </div>
          </button>
        </div>
      </div>
      {reviewingCode && (
        <CodeReview
          onClose={() => setReviewingCode(false)}
          name={name}
          code={code}
          score={numSolved}
          problemID={problemID}
        />
      )}
    </div>
  );
}
