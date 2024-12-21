import React, { useEffect, useState } from "react";
import { collection, doc, getDocs, setDoc } from "firebase/firestore";
import { db } from "./firebase.ts";

interface TestCase {
  params: string;
  jparams: string;
  expected: string;
}

interface Signature {
  python: string;
  java: string;
  javascript: string;
}

// form for adding new questions to database
export default function QuestionForm() {
  const [problemID, setProblemID] = useState("");
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [difficulty, setDifficulty] = useState("Easy");
  const [expectExact, setExpectExact] = useState("True");
  const [returnType, setReturnType] = useState("");
  const [params, setParams] = useState<string[]>([""]);
  const [signature, setSignature] = useState<Signature>({
    python: "",
    java: "",
    javascript: "",
  });
  const [tests, setTests] = useState<TestCase[]>([
    { params: "", jparams: "", expected: "" },
  ]);

  // Fetch the current count of problems to generate problemID
  useEffect(() => {
    const fetchProblemCount = async () => {
      try {
        const snapshot = await getDocs(collection(db, "Problems"));
        const count = snapshot.size; // Get the total number of documents
        setProblemID((count + 1).toString()); // Set the next problemID
      } catch (error) {
        console.error("Error fetching problem count:", error);
      }
    };

    fetchProblemCount();
  }, []);

  // Handle params
  const addParam = () => setParams([...params, ""]);
  const removeParam = (index: number) =>
    setParams(params.filter((_, i) => i !== index));
  const updateParam = (index: number, value: string) => {
    const newParams = [...params];
    newParams[index] = value;
    setParams(newParams);
  };

  // Handle test cases
  const addTestCase = () =>
    setTests([...tests, { params: "", jparams: "", expected: "" }]);

  const removeTestCase = (index: number) =>
    setTests(tests.filter((_, i) => i !== index));

  const updateTestCase = (
    testIndex: number,
    field: keyof TestCase,
    value: string
  ) => {
    const newTests = [...tests];
    newTests[testIndex][field] = value;
    setTests(newTests);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const questionData = {
      problemID,
      name,
      description,
      difficulty,
      expectExact: expectExact === "True",
      signature,
      returnType,
      params,
      tests,
    };

    try {
      // Add the question to the "problems" collection in Firestore
      await setDoc(doc(db, "Problems", name), questionData);
      console.log("Document written with ID: ", name);

      alert("Problem added successfully!");
      resetForm();
    } catch (error) {
      console.error("Error adding document: ", error);
      alert("Failed to add problem. Please try again.");
    }
  };

  const resetForm = () => {
    setProblemID("");
    setName("");
    setDescription("");
    setDifficulty("Easy");
    setExpectExact("True");
    setReturnType("");
    setParams([""]);
    setSignature({ python: "", java: "", javascript: "" });
    setTests([{ params: "", jparams: "", expected: "" }]);
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="p-6 w-full h-full bg-gray-100 rounded-lg shadow-lg overflow-auto text-gray-700"
    >
      <h2 className="text-3xl font-bold mb-6 text-center text-gray-600">
        Create a New Question
      </h2>

      {/* Problem ID */}
      <p className="mb-4 text-gray-600 font-medium">
        Problem ID: {problemID || "Generating..."}
      </p>

      {/* Name */}
      <label className="block mb-4">
        <span className="text-gray-600 font-medium">Name:</span>
        <input
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className="w-full mt-1 p-2 rounded-md border border-gray-300 bg-gray-50"
        />
      </label>

      {/* Description */}
      <label className="block mb-4">
        <span className="text-gray-600 font-medium">Description:</span>
        <textarea
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          className="w-full mt-1 p-2 rounded-md border border-gray-300 bg-gray-50"
          rows={3}
        />
      </label>

      {/* Difficulty */}
      <label className="block mb-4">
        <span className="text-gray-600 font-medium">Difficulty:</span>
        <select
          value={difficulty}
          onChange={(e) => setDifficulty(e.target.value)}
          className="w-full mt-1 p-2 rounded-md border border-gray-300 bg-gray-50"
        >
          <option>Easy</option>
          <option>Medium</option>
          <option>Hard</option>
        </select>
      </label>

      {/* Expect Exact */}
      <label className="block mb-4">
        <span className="text-gray-600 font-medium">Expect Exact:</span>
        <select
          value={expectExact}
          onChange={(e) => setExpectExact(e.target.value)}
          className="w-full mt-1 p-2 rounded-md border border-gray-300 bg-gray-50"
        >
          <option>True</option>
          <option>False</option>
        </select>
      </label>

      {/* Signature */}
      <fieldset className="mb-6">
        <legend className="text-lg font-semibold text-gray-600 mb-2">
          Signature
        </legend>
        {Object.keys(signature).map((lang) => (
          <label key={lang} className="block mb-2">
            <span className="text-gray-600 capitalize">{lang}:</span>
            <input
              type="text"
              value={signature[lang as keyof Signature]}
              onChange={(e) =>
                setSignature({
                  ...signature,
                  [lang]: e.target.value,
                })
              }
              className="w-full mt-1 p-2 rounded-md border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-300 bg-gray-50"
            />
          </label>
        ))}
      </fieldset>

      {/* Return Type */}
      <label className="block mb-6">
        <span className="text-gray-600 font-medium">Return Type:</span>
        <input
          type="text"
          value={returnType}
          onChange={(e) => setReturnType(e.target.value)}
          className="w-full mt-1 p-2 rounded-md border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-300 bg-gray-50"
        />
      </label>

      {/* Parameters */}
      <fieldset className="mb-6">
        <legend className="text-lg font-semibold text-gray-600 mb-2">
          Parameters
        </legend>
        {params.map((param, index) => (
          <div key={index} className="flex items-center mb-2">
            <input
              type="text"
              value={param}
              onChange={(e) => updateParam(index, e.target.value)}
              className="w-full p-2 rounded-md border border-gray-300 bg-gray-50 focus:outline-none"
            />
            <button
              type="button"
              onClick={() => removeParam(index)}
              className="ml-2 p-1 bg-red-400 text-white rounded-md hover:bg-red-500"
            >
              Remove
            </button>
          </div>
        ))}
        <button
          type="button"
          onClick={addParam}
          className="p-2 bg-blue-500 text-white rounded-md hover:bg-blue-600"
        >
          Add Parameter
        </button>
      </fieldset>

      {/* Test Cases */}
      <fieldset className="mb-6">
        <legend className="text-lg font-semibold text-gray-600 mb-2">
          Test Cases
        </legend>
        {tests.map((test, testIndex) => (
          <div
            key={testIndex}
            className="p-4 mb-4 border rounded-md bg-gray-50"
          >
            <label className="block mb-2">
              Params:
              <input
                type="text"
                value={test.params}
                onChange={(e) =>
                  updateTestCase(testIndex, "params", e.target.value)
                }
                className="w-full mt-1 p-2 border rounded-md"
              />
            </label>
            <label className="block mb-2">
              JParams:
              <input
                type="text"
                value={test.jparams}
                onChange={(e) =>
                  updateTestCase(testIndex, "jparams", e.target.value)
                }
                className="w-full mt-1 p-2 border rounded-md"
              />
            </label>
            <label className="block mb-2">
              Expected Output:
              <input
                type="text"
                value={test.expected}
                onChange={(e) =>
                  updateTestCase(testIndex, "expected", e.target.value)
                }
                className="w-full mt-1 p-2 border rounded-md"
              />
            </label>
            <button
              type="button"
              onClick={() => removeTestCase(testIndex)}
              className="p-2 bg-red-400 text-white rounded-md hover:bg-red-500"
            >
              Remove Test Case
            </button>
          </div>
        ))}
        <button
          type="button"
          onClick={addTestCase}
          className="p-2 bg-blue-500 text-white rounded-md hover:bg-blue-600"
        >
          Add Test Case
        </button>
      </fieldset>

      <button
        type="submit"
        className="w-full p-3 bg-green-500 text-white rounded-md font-bold hover:bg-green-600"
      >
        Submit
      </button>
    </form>
  );
}
