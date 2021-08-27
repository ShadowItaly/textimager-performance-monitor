import streamlit as st
import pandas as pd
import json
import numpy as np
import os
import json
st.title("Performance evaluation Module")

dirs = os.listdir( "./experiments" )
experiment = st.selectbox("Select the experiment",dirs)

output_gpu = json.load(open("./experiments/"+experiment+"/output.json","r"))
output_cpu = json.load(open("./experiments/"+experiment+"/output_cpu.json","r"))

data = pd.DataFrame(output_gpu["data"])
data["runtime_cpu"] = pd.DataFrame(output_cpu["data"])["runtime"]
data["runtime_cpu"] = data["runtime_cpu"]/1000000000
data["runtime"] = data["runtime"]/1000000000
st.write("Number of iterations: ",output_cpu["iterations_total"])
st.write("Warumup iterations: ",output_cpu["iterations_warmup"])
with st.beta_expander("Processor details"):
    st.write("Processor: ",output_cpu["processor"])
    st.write("Physical CPU's: ",output_cpu["phys_cpus"])
    st.write("Logical CPU's: ",output_cpu["log_cpus"])
    st.write("CPU frequency (Hz): ",output_cpu["cpu_freq"])

with st.beta_expander("GPU Details"):
    st.write(output_gpu["nvidia-smi"])
st.write(data)

st.markdown("## Evaluation")

for x in pd.unique(data["document_text"]):
    res = data[data["document_text"] == x]
    st.write(res)
    with st.beta_expander("Document text"):
        st.text(x)

    new_df = pd.DataFrame({"runtime_gpu": res["runtime"].to_numpy(), "runtime_cpu": res["runtime_cpu"].to_numpy(), "batch_size": res["batch_size"].to_numpy()})
    new_df = new_df.set_index("batch_size")
    st.line_chart(new_df[["runtime_gpu","runtime_cpu"]])

st.markdown("### Statistics over whole dataset")
st.bar_chart(data.agg({'runtime' : ['max', 'mean', 'min'], 'runtime_cpu': ['max','mean','min']}))
st.bar_chart(data.agg({'runtime' : ['max', 'mean', 'min']}))
st.bar_chart(data.agg({'runtime_cpu' : ['max', 'mean', 'min']}))

st.markdown("Runtime GPU by number of lines")
st.bar_chart(data.groupby(["lines"]).agg({'runtime': ['max','mean','min'],"runtime_cpu":['max','mean','min']})["runtime"])
st.markdown("Runtime CPU by number of lines")
st.bar_chart(data.groupby(["lines"]).agg({'runtime': ['max','mean','min'],"runtime_cpu":['max','mean','min']})["runtime_cpu"])

data['length'] = data['document_text'].apply(lambda x: len(x))
st.markdown("Runtime GPU by number of characters")
st.bar_chart(data.groupby(["length"]).agg({'runtime': ['max','mean','min'],"runtime_cpu":['max','mean','min']})["runtime"])

st.markdown("Runtime CPU by number of characters")
st.bar_chart(data.groupby(["length"]).agg({'runtime': ['max','mean','min'],"runtime_cpu":['max','mean','min']})["runtime_cpu"])
