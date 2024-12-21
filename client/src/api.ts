const HOST = "http://localhost:3232";

// query get api endpoint
export async function queryAPI(
  endpoint: string,
  query_params?: Record<string, string>
) {
  const paramsString = new URLSearchParams(query_params).toString();
  const url = `${HOST}/${endpoint}?${paramsString}`;
  const response = await fetch(url);
  if (!response.ok) {
    console.error(response.status, response.statusText);
  }
  return response.json();
}

// query post api endpoint
export async function queryAPIPost(
  endpoint: string,
  body: Record<string, any>
) {
  const response = await fetch(`${HOST}/${endpoint}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    console.error(response.status, response.statusText);
  }
  return response.json();
}
